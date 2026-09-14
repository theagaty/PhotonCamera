from pathlib import Path


def replace_once(path, old, new, label):
    p = Path(path)
    text = p.read_text()
    if text.count(old) != 1:
        raise SystemExit(f"{label}: expected one match, found {text.count(old)}")
    p.write_text(text.replace(old, new, 1))
    print(f"Applied: {label}")


main = "app/src/main/java/com/hinnka/mycamera/MainActivity.kt"
gallery = "app/src/main/java/com/hinnka/mycamera/ui/gallery/GalleryScreen.kt"
detail = "app/src/main/java/com/hinnka/mycamera/ui/gallery/GalleryDetailScreen.kt"

replace_once(
    main,
    "    val currentRoute = navBackStackEntry?.destination?.route\n    val handleGalleryBack: () -> Unit = {\n",
    "    val currentRoute = navBackStackEntry?.destination?.route\n\n"
    "    LaunchedEffect(currentRoute) {\n"
    "        val rotateWorkspace = currentRoute == Routes.GALLERY ||\n"
    "            currentRoute == Routes.PHOTO_DETAIL ||\n"
    "            currentRoute == Routes.BURST_DETAIL ||\n"
    "            currentRoute == Routes.PHOTO_EDIT ||\n"
    "            currentRoute == Routes.SETTINGS\n"
    "        (context as? MainActivity)?.requestedOrientation = if (rotateWorkspace) {\n"
    "            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR\n"
    "        } else {\n"
    "            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT\n"
    "        }\n"
    "    }\n\n"
    "    val handleGalleryBack: () -> Unit = {\n",
    "route-aware workspace rotation",
)

replace_once(gallery, "import com.hinnka.mycamera.utils.OrientationObserver\n", "", "remove legacy orientation import")
replace_once(
    gallery,
    "                    isLandscape = OrientationObserver.isLandscape,\n                    rotationDegrees = OrientationObserver.rotationDegrees,\n",
    "                    isLandscape = false,\n                    rotationDegrees = 0f,\n",
    "disable manual thumbnail rotation",
)
replace_once(gallery, "        val isRotated = OrientationObserver.isLandscape\n", "        val isRotated = false\n", "disable rotated child measurement")
replace_once(
    gallery,
    "    return if (OrientationObserver.isLandscape) {\n        resolvedHeight.toFloat() / resolvedWidth.toFloat()\n    } else {\n        resolvedWidth.toFloat() / resolvedHeight.toFloat()\n    }\n",
    "    return resolvedWidth.toFloat() / resolvedHeight.toFloat()\n",
    "use natural thumbnail aspect ratio",
)

p = Path(detail)
text = p.read_text()
if text.count("modifier = Modifier.autoRotate()") != 4:
    raise SystemExit(f"detail autoRotate count mismatch: {text.count('modifier = Modifier.autoRotate()')}")
text = text.replace("modifier = Modifier.autoRotate()", "modifier = Modifier")
text = text.replace("import com.hinnka.mycamera.ui.camera.autoRotate\n", "")
p.write_text(text)
print("Applied: GalleryDetail real-layout rotation")
