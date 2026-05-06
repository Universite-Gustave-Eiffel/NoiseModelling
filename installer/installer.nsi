

!define APP_NAME        "NoiseModelling"
!define APP_PUBLISHER   "Noise-Planet"
!define APP_URL         "https://noise-planet.org"
!define EXE_NAME        "NoiseModelling.exe"
!define INSTALLER_NAME "NoiseModelling-${APP_VERSION}-Setup.exe"
!define INSTALL_DIR     "$PROGRAMFILES64\${APP_NAME}"
!define UNINSTALL_KEY   "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_NAME}"


!ifndef APP_VERSION
  !define APP_VERSION "0.0.0"
!endif


Name            "${APP_NAME} ${APP_VERSION}"
OutFile         "${INSTALLER_NAME}"
InstallDir      "${INSTALL_DIR}"
InstallDirRegKey HKLM "${UNINSTALL_KEY}" "InstallLocation"
RequestExecutionLevel admin
SetCompressor   /SOLID lzma
Unicode         True


!include "MUI2.nsh"

!define MUI_ABORTWARNING
!define MUI_ICON          "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"
!define MUI_UNICON        "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall.ico"

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

  SetOutPath "$INSTDIR"
  File /r "installer\app\*"
  File "NoiseModelling.exe"
  

  SetOutPath "$INSTDIR\jre"
  File /r "jre\*.*"  


  CreateShortcut "$DESKTOP\${APP_NAME}.lnk" \
                 "$INSTDIR\${EXE_NAME}" \
                 "" \
                 "$INSTDIR\${EXE_NAME}" 0


  CreateDirectory "$SMPROGRAMS\${APP_NAME}"
  CreateShortcut  "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk" \
                  "$INSTDIR\${EXE_NAME}" \
                  "" \
                  "$INSTDIR\${EXE_NAME}" 0
  CreateShortcut  "$SMPROGRAMS\${APP_NAME}\Uninstall ${APP_NAME}.lnk" \
                  "$INSTDIR\Uninstall.exe"

  WriteUninstaller "$INSTDIR\Uninstall.exe"

  WriteRegStr   HKLM "${UNINSTALL_KEY}" "DisplayName"      "${APP_NAME} ${APP_VERSION}"
  WriteRegStr   HKLM "${UNINSTALL_KEY}" "DisplayVersion"   "${APP_VERSION}"
  WriteRegStr   HKLM "${UNINSTALL_KEY}" "Publisher"        "${APP_PUBLISHER}"
  WriteRegStr   HKLM "${UNINSTALL_KEY}" "URLInfoAbout"     "${APP_URL}"
  WriteRegStr   HKLM "${UNINSTALL_KEY}" "InstallLocation"  "$INSTDIR"
  WriteRegStr   HKLM "${UNINSTALL_KEY}" "UninstallString"  "$INSTDIR\Uninstall.exe"
  WriteRegDWORD HKLM "${UNINSTALL_KEY}" "NoModify"         1
  WriteRegDWORD HKLM "${UNINSTALL_KEY}" "NoRepair"         1

SectionEnd

Section "Uninstall"

  Delete "$INSTDIR\${EXE_NAME}"
  Delete "$INSTDIR\Uninstall.exe"
  RMDir  "$INSTDIR"


  Delete "$DESKTOP\${APP_NAME}.lnk"
  Delete "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk"
  Delete "$SMPROGRAMS\${APP_NAME}\Uninstall ${APP_NAME}.lnk"
  RMDir  "$SMPROGRAMS\${APP_NAME}"

  DeleteRegKey HKLM "${UNINSTALL_KEY}"

SectionEnd
