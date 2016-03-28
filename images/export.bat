@echo off

REM goto :eof

call :export rings
call :export snapshot_button
REM call :export video_button
REM call :export white_plus
call :export white_x
goto :eof

:export
REM inkscape -f images.svg -i %~1 -j -d 45 -e ..\app\src\main\res\%~1_16.png
REM inkscape -f images.svg -i %~1 -j -d 50.625 -e ..\app\src\main\res\%~1_18.png
REM inkscape -f images.svg -i %~1 -j -d 67.5 -e ..\app\src\main\res\%~1_24.png
REM inkscape -f images.svg -i %~1 -j -d 78.75 -e ..\app\src\main\res\%~1_28.png
REM inkscape -f images.svg -i %~1 -j -d 157.5 -e ..\app\src\main\res\%~1_56.png
REM inkscape -f images.svg -i %~1 -j -d 101.25 -e ..\app\src\main\res\%~1_36.png
REM inkscape -f images.svg -i %~1 -j -d 720 -e ..\app\src\main\res\%~1_256.png
inkscape -f images.svg -i %~1 -j -d 90 -e ..\app\src\main\res\drawable-mdpi\%~1.png
inkscape -f images.svg -i %~1 -j -d 135 -e ..\app\src\main\res\drawable-hdpi\%~1.png
inkscape -f images.svg -i %~1 -j -d 180 -e ..\app\src\main\res\drawable-xhdpi\%~1.png
inkscape -f images.svg -i %~1 -j -d 270 -e ..\app\src\main\res\drawable-xxhdpi\%~1.png
inkscape -f images.svg -i %~1 -j -d 360 -e ..\app\src\main\res\drawable-xxxhdpi\%~1.png
goto :eof
