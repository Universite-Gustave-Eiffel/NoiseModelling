

!define APP_NAME        "NoiseModelling"
!define APP_PUBLISHER   "Noise-Planet"
!define APP_URL         "https://noise-planet.org"
!define EXE_NAME        "NoiseModelling.exe"
!ifndef MAVEN_VERSION
  !define MAVEN_VERSION "dev"
!endif
!define INSTALLER_NAME "NoiseModelling-${MAVEN_VERSION}-Setup.exe"
!define INSTALL_DIR     "$LOCALAPPDATA\${APP_NAME}"
!define UNINSTALL_KEY   "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}"


!ifndef APP_VERSION
  !define APP_VERSION "0.0.0"
!endif


Name            "${APP_NAME} ${MAVEN_VERSION}"
OutFile         "${INSTALLER_NAME}"
InstallDir      "${INSTALL_DIR}"
InstallDirRegKey HKCU "${UNINSTALL_KEY}" "InstallLocation"
RequestExecutionLevel user
SetCompressor   /SOLID lzma
Unicode         True


!include "MUI2.nsh"

!define MUI_ABORTWARNING
!define MUI_ICON          "noisemodelling.ico"
!define MUI_UNICON        "noisemodelling.ico"

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE               "..\LICENSE"
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "French"
!insertmacro MUI_LANGUAGE "English"

Section "NoiseModelling" SecMain
  SectionIn RO

  SetShellVarContext current
  SetOutPath "$INSTDIR"
  File /r "app\*"
  File "NoiseModelling.exe"
  File "noisemodelling.ico"
  

  SetOutPath "$INSTDIR\jre"
  File /r "jre\*.*"  


  CreateShortcut "$DESKTOP\${APP_NAME}.lnk" \
                 "$INSTDIR\${EXE_NAME}" \
                 "" \
                 "$INSTDIR\noisemodelling.ico" 0


  CreateDirectory "$SMPROGRAMS\${APP_NAME}"
  CreateShortcut  "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk" \
                  "$INSTDIR\${EXE_NAME}" \
                  "" \
                  "$INSTDIR\noisemodelling.ico" 0
  CreateShortcut  "$SMPROGRAMS\${APP_NAME}\Uninstall ${APP_NAME}.lnk" \
                  "$INSTDIR\Uninstall.exe"

  WriteUninstaller "$INSTDIR\Uninstall.exe"

  WriteRegStr   HKCU "${UNINSTALL_KEY}" "DisplayName"      "${APP_NAME} ${MAVEN_VERSION}"
  WriteRegStr   HKCU "${UNINSTALL_KEY}" "DisplayVersion"   "${MAVEN_VERSION}"
  WriteRegStr   HKCU "${UNINSTALL_KEY}" "Publisher"        "${APP_PUBLISHER}"
  WriteRegStr   HKCU "${UNINSTALL_KEY}" "URLInfoAbout"     "${APP_URL}"
  WriteRegStr   HKCU "${UNINSTALL_KEY}" "InstallLocation"  "$INSTDIR"
  WriteRegStr   HKCU "${UNINSTALL_KEY}" "UninstallString"  "$INSTDIR\Uninstall.exe"
  WriteRegDWORD HKCU "${UNINSTALL_KEY}" "NoModify"         1
  WriteRegDWORD HKCU "${UNINSTALL_KEY}" "NoRepair"         1

SectionEnd

Section "Uninstall"
  SetShellVarContext current

  Delete "$INSTDIR\${EXE_NAME}"
  Delete "$INSTDIR\noisemodelling.ico"
  Delete "$INSTDIR\Uninstall.exe"
  RMDir /r "$INSTDIR"


  Delete "$DESKTOP\${APP_NAME}.lnk"
  Delete "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk"
  Delete "$SMPROGRAMS\${APP_NAME}\Uninstall ${APP_NAME}.lnk"
  RMDir  "$SMPROGRAMS\${APP_NAME}"

  DeleteRegKey HKCU "${UNINSTALL_KEY}"
SectionEnd
