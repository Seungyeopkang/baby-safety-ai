import os
import cv2
import time
import torch
import argparse
import numpy as np
import streamlit as st
import tempfile

from Detection.Utils import ResizePadding
from CameraLoader import CamLoader, CamLoader_Q
from DetectorLoader import TinyYOLOv3_onecls

from PoseEstimateLoader import SPPE_FastPose
from fn import draw_single

from Track.Tracker import Detection, Tracker
from ActionsEstLoader import TSSTG

def preproc(image):
    """preprocess function for CameraLoader."""
    image = resize_fn(image)
    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    return image

def kpt2bbox(kpt, ex=20):
    """Get bbox that hold on all of the keypoints (x,y)
    kpt: array of shape `(N, 2)`,
    ex: (int) expand bounding box,
    """
    return np.array((kpt[:, 0].min() - ex, kpt[:, 1].min() - ex,
                     kpt[:, 0].max() + ex, kpt[:, 1].max() + ex))

@st.cache_resource
def load_models(device):
    # DETECTION MODEL.
    inp_dets = 384
    detect_model = TinyYOLOv3_onecls(inp_dets, device=device)

    # POSE MODEL.
    inp_pose = (224, 160)
    pose_model = SPPE_FastPose('resnet50', inp_pose[0], inp_pose[1], device=device)

    # Actions Estimate.
    action_model = TSSTG()

    return detect_model, pose_model, action_model

if __name__ == '__main__':
    st.title("실시간 넘어짐 감지")

    device = 'cuda' if st.sidebar.checkbox("Use CUDA", value=True) else 'cpu'
    show_detected = st.sidebar.checkbox("Show Detected Bounding Boxes", value=False)
    show_skeleton = st.sidebar.checkbox("Show Skeleton Pose", value=True)
    save_out = st.sidebar.text_input("Save Output Video (optional, path):", "")

    detect_model, pose_model, action_model = load_models(device)
    resize_fn = ResizePadding(384, 384)
    tracker = Tracker(max_age=30, n_init=3)
    outvid = False
    writer = None
    if save_out:
        outvid = True
        codec = cv2.VideoWriter_fourcc(*'MJPG')
        # 임시로 프레임 크기를 설정, 실제 크기는 비디오에서 얻어야 함
        writer = cv2.VideoWriter(save_out, codec, 30, (384 * 2, 384 * 2))

    video_source = st.radio("비디오 소스 선택:", ("웹캠", "비디오 파일", "샘플 비디오"))

    if video_source == "웹캠":
        video_capture = cv2.VideoCapture(0)
        if not video_capture.isOpened():
            st.error("웹캠을 열 수 없습니다.")
            st.stop()
    elif video_source == "비디오 파일":
        uploaded_file = st.file_uploader("비디오 파일을 업로드하세요", type=["mp4", "avi"])
        if uploaded_file is not None:
            tfile = tempfile.NamedTemporaryFile(delete=False)
            tfile.write(uploaded_file.read())
            video_capture = cv2.VideoCapture(tfile.name)
            if not video_capture.isOpened():
                st.error("비디오 파일을 열 수 없습니다.")
                st.stop()
        else:
            st.warning("비디오 파일을 업로드해주세요.")
            st.stop()
    elif video_source == "샘플 비디오":
        video_capture = cv2.VideoCapture('../Data/falldata/Home/Videos/video (1).avi')
        if not video_capture.isOpened():
            st.error("샘플 비디오를 열 수 없습니다.")
            st.stop()

    output_image_placeholder = st.empty()
    fps_time = 0
    f = 0
    while video_capture.isOpened():
        f += 1
        ret, frame = video_capture.read()
        if not ret:
            st.warning("비디오 프레임을 읽을 수 없습니다. 스트림 종료.")
            break

        image = frame.copy()
        frame_processed = frame.copy()

        # Detect humans bbox in the frame with detector model.
        detected = detect_model.detect(frame, need_resize=False, expand_bb=10)

        # Predict each tracks bbox of current frame from previous frames information with Kalman filter.
        tracker.predict()
        # Merge two source of predicted bbox together.
        for track in tracker.tracks:
            det = torch.tensor([track.to_tlbr().tolist() + [0.5, 1.0, 0.0]], dtype=torch.float32)
            detected = torch.cat([detected, det], dim=0) if detected is not None else det

        detections = []  # List of Detections object for tracking.
        if detected is not None:
            poses = pose_model.predict(frame, detected[:, 0:4], detected[:, 4])
            detections = [Detection(kpt2bbox(ps['keypoints'].numpy()),
                                     np.concatenate((ps['keypoints'].numpy(),
                                                     ps['kp_score'].numpy()), axis=1),
                                     ps['kp_score'].mean().numpy()) for ps in poses]

            # VISUALIZE DETECTED BBOX.
            if show_detected:
                for bb in detected[:, 0:5]:
                    frame_processed = cv2.rectangle(frame_processed, (int(bb[0]), int(bb[1])), (int(bb[2]), int(bb[3])), (0, 0, 255), 1)

        # Update tracks.
        tracker.update(detections)

        # Predict Actions of each track.
        for i, track in enumerate(tracker.tracks):
            if not track.is_confirmed():
                continue

            track_id = track.track_id
            bbox = track.to_tlbr().astype(int)
            center = track.get_center().astype(int)

            action = 'pending..'
            clr = (0, 255, 0)
            if len(track.keypoints_list) == 30:
                pts = np.array(track.keypoints_list, dtype=np.float32)
                out = action_model.predict(pts, frame.shape[:2])
                action_name = action_model.class_names[out[0].argmax()]
                action = '{}: {:.2f}%'.format(action_name, out[0].max() * 100)
                if action_name == 'Fall Down':
                    clr = (255, 0, 0)
                elif action_name == 'Lying Down':
                    clr = (255, 200, 0)

            # VISUALIZE TRACKS.
            if track.time_since_update == 0:
                if show_skeleton and len(track.keypoints_list) > 0:
                    frame_processed = draw_single(frame_processed, track.keypoints_list[-1])
                frame_processed = cv2.rectangle(frame_processed, (bbox[0], bbox[1]), (bbox[2], bbox[3]), (0, 255, 0), 1)
                frame_processed = cv2.putText(frame_processed, str(track_id), (center[0], center[1]), cv2.FONT_HERSHEY_COMPLEX,
                                            0.4, (255, 0, 0), 2)
                frame_processed = cv2.putText(frame_processed, action, (bbox[0] + 5, bbox[1] + 15), cv2.FONT_HERSHEY_COMPLEX,
                                            0.4, clr, 1)

        # Show Frame.
        frame_processed = cv2.resize(frame_processed, (0, 0), fx=1.5, fy=1.5)
        frame_processed = cv2.putText(frame_processed, '%d, FPS: %f' % (f, 1.0 / (time.time() - fps_time + 1e-7)),
                                    (10, 20), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)
        frame_processed = frame_processed[:, :, ::-1]
        fps_time = time.time()

        output_image_placeholder.image(frame_processed, channels="RGB")

        if outvid and writer is not None:
            writer.write(frame_processed)

    video_capture.release()
    if outvid and writer is not None:
        writer.release()