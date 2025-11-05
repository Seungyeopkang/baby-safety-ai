# 👶 AI 유아 안전 모니터링 및 실시간 대응 시스템

![프로젝트 개요 포스터](figures/AI_유아_안전_모니터링_및_실시간_대응_시스템.jpg)

---

## 🧐 프로젝트 개요 (Project Overview)

본 프로젝트는 AI 기술을 활용하여 유아 시설 내의 안전사고를 실시간으로 탐지하고 대응하는 자동화 모니터링 시스템입니다.

### 1. 문제 배경 (Motivation)

* **높은 사고 비율:** 유아 안전사고는 꾸준히 높은 비율로 발생하며, 특히 유아 시설 내에서의 **낙상** 사고와 **인원 초과**로 인한 사고 위험이 심각한 문제로 대두되고 있습니다.
* **관리 인력의 한계:** 2025년까지 인건비가 약 15% 증가할 것으로 예상되는 등, 유아 시설은 인력 확보에 큰 어려움을 겪고 있습니다.
* **대응의 어려움:** 이러한 관리 인력의 부족은 24시간 실시간 감시를 어렵게 만들며, 사고 발생 시 즉각적인 대응을 놓치는 상황으로 이어집니다.

### 2. 프로젝트 목표 (Goal)

이러한 문제를 해결하기 위해, 본 시스템은 **넘어짐, 인원 초과, 울음소리** 등 유아의 위험 및 이상 상황을 AI가 실시간으로 자동 탐지합니다.

사고 발생 즉시 관리자의 모바일 앱으로 알림을 전송하여, **관리 인력의 부담을 획기적으로 줄이고** 유아 안전을 강화하는 것을 목표로 합니다.

## ✨ 주요 기능 (Features)

* **넘어짐 감지 (Fall Detection):** CCTV 영상 속 유아의 자세를 분석하여 넘어짐 사고를 실시간으로 탐지합니다.
* **인원 초과 감지 (Overcrowding Detection):** 설정된 공간 내의 인원수를 실시간으로 파악하여, 기준 인원 초과 시 알림을 보냅니다.
* **울음소리 감지 (Crying Detection):** 현장의 소리를 분석하여 유아의 울음소리를 식별합니다.
* **실시간 알림 (Real-time Alert):** 위험 상황(인원 초과, 또는 넘어짐과 울음 동시 발생) 탐지 시, 즉시 모바일 앱으로 푸시 알림을 전송합니다.
* **영상 확인 (Video Confirmation):** 관리자는 알림 수신 즉시 앱을 통해 현장 영상을 확인하여 신속하게 대응할 수 있습니다.

## ⚙️ 시스템 아키텍처 (System Architecture)

본 시스템은 논문의 [그림 1]을 기반으로 다음과 같이 구성됩니다.

| 컴포넌트 | 기술 스택 | 주요 역할 |
| :--- | :--- | :--- |
| **CCTV** | IP Camera | RTSP 프로토콜을 통해 실시간 영상 스트림 전송 |
| **AI Server** | FastAPI | (Python) 영상 스트림 수신, AI 모델(YOLO, SPPE, ST-GCN, YAMNet)을 통한 위험 상황 분석 |
| **Backend Server** | Spring Boot | (Java) 비즈니스 로직 처리, DB 연동, AI 서버 및 클라이언트 앱과 API 통신 |
| **Client App** | Flutter | (Dart) 관리자용 모바일 앱. 푸시 알림 수신 및 영상 확인 |
| **Database** | MySQL | 사용자 정보, 이벤트 기록(타임스탬프), 영상 URL 등 저장 |
| **Alert System** | FCM | (Firebase Cloud Messaging) 백엔드 서버의 요청을 받아 클라이언트 앱으로 푸시 알림 전송 |

## 🧠 AI 모델 파이프라인 (AI Pipeline)

### 1. 넘어짐 및 인원 초과 감지
영상 프레임을 입력받아 3단계의 파이프라인을 거칩니다.

1.  **`YOLO`:** 프레임 내 모든 사람을 탐지(Bounding Box)하고, 동시에 인원수를 카운트하여 **인원 초과** 여부를 판단합니다.
2.  **`SPPE` (AlphaPose):** 탐지된 각 사람의 2D 관절점(Skeleton)을 추출합니다.
3.  **`ST-GCN`:** 추출된 관절점의 시공간적(Spatio-Temporal) 움직임을 분석하여 **'넘어짐'** 행동을 최종 분류합니다.

### 2. 울음소리 감지
1.  **`YAMNet` (Transfer Learning):** 다양한 오디오 분류에 사전 학습된 YAMNet 모델을 유아 울음소리 데이터로 미세 조정(Fine-tuning)하여 **'울음소리'** 여부를 분류합니다.
2.  **최종 알림:** **(인원 초과 시)** 또는 **(넘어짐 + 울음소리 동시 감지 시)** 관리자에게 알림이 전송됩니다.

## 🛠️ 설치 및 실행 방법 (Setup & Run)

이 프로젝트는 3개의 독립적인 서버(AI, Backend, Frontend)로 구성되어 있습니다.

> ℹ️ **[참고]** 각 파트의 폴더 이름 (`Human_Falling_Detect`, `backend` 등)은 실제 프로젝트 구조에 맞게 수정해주세요.

### 1. AI Server (넘어짐 탐지 / Fall Detection)

넘어짐 탐지 AI 서버는 `Human_Falling_Detect/Human-Falling-Detect/Human-Falling-Detect-Tracks-master` 폴더에 있습니다.

#### 사전 준비 (Prerequisites)

1.  **폴더로 이동**

    ```bash
    cd Human_Falling_Detect/Human-Falling-Detect/Human-Falling-Detect-Tracks-master
    ```

2.  **필수 라이브러리 설치**
    `requirements.txt`에 모든 패키지가 있지만, 먼저 핵심 라이브러리를 설치하는 것을 권장합니다.

    ```bash
    pip install torch torchvision opencv-python streamlit
    ```
    > ⚠️ **참고:** 실행 중 `ModuleNotFoundError`가 발생하면 `pip install <package-name>`으로 필요한 패키지를 추가로 설치해주세요.

3.  **미리 훈련된 모델 다운로드**
    아래 링크에서 모델 파일들을 다운로드한 후, `Models/` 폴더를 생성하여 그 안에 모두 넣어주세요.

    *   **Tiny-YOLOv3**: [.pth file](https://drive.google.com/file/d/1obEbWBSm9bXeg10FriJ7R2cGLRsg-AfP/view?usp=sharing), [.cfg file](https://drive.google.com/file/d/19sPzBZjAjuJQ3emRteHybm2SG25w9Wn5/view?usp=sharing)
    *   **SPPE (AlphaPose)**: [resnet101](https://drive.google.com/file/d/1N2MgE1Esq6CKYA6FyZVKpPwHRyOCrzA0/view?usp=sharing), [resnet50](https://drive.google.com/file/d/1IPfCDRwCmQDnQy94nT1V-_NVtTEi4VmU/view?usp=sharing)
    *   **ST-GCN**: [tsstg](https://drive.google.com/file/d/1mQQ4JHe58ylKbBqTjuKzpwN2nwKOWJ9u/view?usp=sharing)

#### 데모 실행 (Running the Demo)

이 프로젝트는 세 가지 방식으로 실행할 수 있습니다.

##### 방법 1: 커맨드 라인 (권장)

터미널에서 `main.py`를 실행하는 가장 기본적인 방법입니다.

```bash
# 비디오 파일을 사용할 경우
python main.py --camera "비디오 파일 경로"

# 웹캠을 사용할 경우 (카메라 ID 0)
python main.py --camera 0
```

##### 방법 2: GUI 애플리케이션

`Tkinter`로 만든 GUI 창에서 데모를 실행합니다.

```bash
python App.py
```

> **참고**: `App.py`는 코드에 하드코딩된 샘플 비디오를 재생합니다. 다른 비디오를 사용하려면 `App.py` 파일의 `self.load_cam(...)` 부분을 직접 수정해야 합니다.

##### 방법 3: 웹 애플리케이션

`Streamlit`을 사용하여 웹 브라우저에서 인터랙티브한 데모를 실행합니다.

```bash
streamlit run main2.py
```
명령어 실행 후, 웹 브라우저에서 **웹캠**, **파일 업로드**, **샘플 비디오** 중 원하는 입력 소스를 선택하여 테스트할 수 있습니다.