import cv2
import numpy as np


def is_daytime(frame):
    height, width, _ = frame.shape
    sky_region = frame[:height//3, :]
    gray = cv2.cvtColor(sky_region, cv2.COLOR_BGR2GRAY)
    brightness = np.mean(gray)
    return brightness > 100

def is_nighttime(frame):
    height, width, _ = frame.shape
    sky_region = frame[:height//3, :]
    gray = cv2.cvtColor(sky_region, cv2.COLOR_BGR2GRAY)
    brightness = np.mean(gray)

    if brightness < 50:
        _, thresholded = cv2.threshold(gray, 200, 255, cv2.THRESH_BINARY)
        bright_pixels = np.sum(thresholded == 255)
        total_pixels = gray.size
        bright_ratio = bright_pixels / total_pixels

        if bright_ratio < 0.01:
            return True
    return False

def compute_histogram(frame):
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    hist = cv2.calcHist([gray], [0], None, [256], [0, 256])
    cv2.normalize(hist, hist)
    return hist

def compare_histograms(hist1, hist2):
    score = cv2.compareHist(hist1, hist2, cv2.HISTCMP_CORREL)
    return score

def detect_rain(hist1, hist2, threshold=0.8):
    score = compare_histograms(hist1, hist2)
    return score < threshold

def process_video_stream(video_path):
    cap = cv2.VideoCapture(video_path)

    if not cap.isOpened():
        print("Error: Could not open video file.")
        return

    ret, prev_frame = cap.read()
    if not ret:
        print("Error: Could not read the first frame.")
        return

    prev_hist = compute_histogram(prev_frame)

    fps = cap.get(cv2.CAP_PROP_FPS)
    rain_detected = False
    no_rain_frames = 0
    no_rain_frame_threshold = 30
    check_interval = 150
    frame_count = 0


    daytime = is_daytime(prev_frame)
    nighttime = not daytime

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        frame_count += 1


        if frame_count % check_interval == 0:
            daytime = is_daytime(frame)
            nighttime = not daytime

        current_hist = compute_histogram(frame)
        raining = detect_rain(prev_hist, current_hist)

        condition = ""
        if daytime:
            condition = "DayMode"
        elif nighttime:
            condition = "NightMode"

        if raining:
            rain_detected = True
            no_rain_frames = 0
        else:
            no_rain_frames += 1

        if rain_detected:
            condition += " - Rain Detected"

        if no_rain_frames > no_rain_frame_threshold:
            rain_detected = False

        cv2.putText(frame, condition, (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 0, 0), 2)
        cv2.imshow("Road Condition", frame)

        delay = int(500 / fps)
        if cv2.waitKey(delay) & 0xFF == ord('q'):
            break

        prev_hist = current_hist

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    video_path = "dataset/blur/blur_appear_rainy2.mp4"
    process_video_stream(video_path)
