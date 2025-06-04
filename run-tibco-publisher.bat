@echo off
echo ========================================
echo    TIBCO EMS Standalone Publisher
echo ========================================
echo.
echo Starting TIBCO EMS Publisher...
echo Target Queue: TEST.QUEUE.1
echo Server: tcp://localhost:7222
echo.

cd backend
mvn spring-boot:run -Dspring-boot.run.main-class=com.service.virtualization.tibco.test.TibcoEMSStandalonePublisher

pause 