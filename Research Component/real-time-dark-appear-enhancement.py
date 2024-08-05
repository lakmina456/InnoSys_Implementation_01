import cv2
import numpy as np
from skimage import restoration
import sys

EPSILON = sys.float_info.epsilon


blur_threshold = 100  # Example threshold for blur
# Environment Detection Module
def is_daytime(frame):
    height, width, _ = frame.shape
    sky_region = frame[:height // 3, :]
    gray = cv2.cvtColor(sky_region, cv2.COLOR_BGR2GRAY)
    brightness = np.mean(gray)
    return brightness > 100

def is_nighttime(frame):
    height, width, _ = frame.shape
    sky_region = frame[:height // 3, :]
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

# Function to detect darkness
def detect_darkness(frame):
    # Convert the image to grayscale if it is not already
    if len(frame.shape) == 3:
        frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

    # Apply Otsu's thresholding
    _, thresholded = cv2.threshold(frame, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

    # Calculate the percentage of dark pixels
    dark_pixels = np.sum(thresholded == 0)
    total_pixels = frame.size
    dark_percentage = dark_pixels / total_pixels

    # Define a darkness threshold (e.g., 0.5 for 50%)
    darkness_threshold = 0.5

    dark = dark_percentage > darkness_threshold
    if dark:
        return True
    else:
        return False

def detect_blur(frame):
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    fm = cv2.Laplacian(gray, cv2.CV_64F).var()

    if fm < blur_threshold:
        return True
    else:
        return False
class ImageDeblur:
    def __init__(self, psf_size=(5, 5)):
        self.psf = np.ones(psf_size) / np.prod(psf_size)

    def preprocess(self, image):
        image = image / 255.
        image[image >= 1] = 1 - EPSILON
        return image

    def deblur(self, image):
        B = image[:, :, 0]
        G = image[:, :, 1]
        R = image[:, :, 2]
        deconvolved_B = restoration.wiener(B, self.psf, 1, clip=False)
        deconvolved_G = restoration.wiener(G, self.psf, 1, clip=False)
        deconvolved_R = restoration.wiener(R, self.psf, 1, clip=False)
        rgbArray = np.zeros((image.shape[0], image.shape[1], 3))
        rgbArray[..., 0] = deconvolved_B
        rgbArray[..., 1] = deconvolved_G
        rgbArray[..., 2] = deconvolved_R
        return rgbArray

    def f_inverse(self, f, a=2.5, c=0.6, func=None):
        if func is not None:
            return func(f)
        f_cth_root = np.power(f, 1 / c)
        return np.power(f_cth_root / (1 - f_cth_root), 1 / a)

    def lightspace_deblur(self, image):
        left_half_width = image.shape[1] // 2
        left_side = image[:, :left_half_width, :]
        enhanced_left_side = self.f_inverse(left_side)
        enhanced_image = np.copy(image)
        enhanced_image[:, :left_half_width, :] = enhanced_left_side
        enhanced_image[enhanced_image >= 1] = 1 - EPSILON  # Ensure values are within [0, 1]
        return enhanced_image

def process_video_realtime(input_video_path):
    cap = cv2.VideoCapture(input_video_path)

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

        # Perform preprocessing checks
        is_dark = detect_darkness(frame)
        #is_blurry = detect_blur(frame)

        # Example usage

        image_deblur = ImageDeblur()
        # Display detection results on the frame window
        cv2.putText(frame, condition, (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 0, 0), 2)
        cv2.putText(frame, f"Darkness Appear: {is_dark}", (10, 70), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
        #cv2.putText(frame, f"Blurry Appear: {is_blurry}", (10, 70), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)

        # Apply enhancement if darkness is detected
        if is_dark:
            preprocessed_frame = image_deblur.preprocess(frame)
            enhanced_frame = image_deblur.lightspace_deblur(preprocessed_frame)
            frame = (enhanced_frame * 255).astype(np.uint8)

        # Display the frame
        cv2.imshow('Frame', frame)

        # Break the loop on key press (optional)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break
        delay = int(500 / fps)
        if cv2.waitKey(delay) & 0xFF == ord('q'):
            break

        prev_hist = current_hist
    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    process_video_realtime('G:/dataset/pythonProject/day.mp4')