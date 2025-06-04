@echo off
echo ========================================
echo    TIBCO EMS Standalone Consumer
echo ========================================
echo.
echo Starting TIBCO EMS Consumer...
echo Source Queue: TEST.QUEUE.1
echo Server: tcp://localhost:7222
echo.

cd backend
mvn spring-boot:run -Dspring-boot.run.main-class=com.service.virtualization.tibco.test.TibcoEMSStandaloneConsumer

pause 