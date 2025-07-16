from pathlib import Path
import shutil

REPO_APK_DIR = Path("repo/apk")

try:
    shutil.rmtree(REPO_APK_DIR)
except FileNotFoundError:
    pass

REPO_APK_DIR.mkdir(parents=True, exist_ok=True)

apk_artifacts_dir = Path.home() / "apk-artifacts"
print(f"Searching for APKs recursively in: {apk_artifacts_dir}")

apks = list(apk_artifacts_dir.rglob("*.apk"))
print(f"Found APK files: {[str(apk) for apk in apks]}")

for apk in apks:
    apk_name = apk.name.replace("-release.apk", ".apk")
    print(f"Moving {apk} to {REPO_APK_DIR / apk_name}")
    shutil.move(apk, REPO_APK_DIR / apk_name)

for apk in (Path.home() / "apk-artifacts").rglob("*.apk"):
    apk_name = apk.name.replace("-release.apk", ".apk")

    shutil.move(apk, REPO_APK_DIR / apk_name)