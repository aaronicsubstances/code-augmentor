@echo off
set args=%1
shift
:start
if [%1] == [] goto done
set args=%args% %1
shift
goto start

:done
rem (use %args% here)
set coreCp=..\code-augmentor-core\build\libs\code-augmentor-core-2.0.0.jar
groovy -cp "%coreCp%" %args%