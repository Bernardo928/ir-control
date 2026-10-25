package com.bernardo.ircontrol

/** Teclas do controle e os nomes que elas costumam ter no IR-DB (já normalizados, ver [normalizeName]). */
enum class RemoteKey(vararg val aliases: String) {
    POWER("POWER", "POWERTOGGLE", "POWERONOFF", "ONOFF", "STANDBY", "POWERON", "ON", "PWR", "TVPOWER", "POWERTV"),
    GUIDE("GUIDE", "EPG", "TVGUIDE", "PROGRAMGUIDE"),
    HDMI("HDMI", "HDMI1", "HDMI2", "INPUTHDMI", "INPUTHDMI1"),
    HOME("HOME", "SMARTHUB", "SMART", "HOMEMENU", "PORTAL", "NETCAST", "SMARTTV", "APPS"),
    TOOLS("TOOLS", "OPTIONS", "OPTION", "QMENU", "QUICKMENU", "QUICK", "TOOL"),
    INFO("INFO", "DISPLAY", "OSD", "INFORMATION", "STATUS"),
    UP("UP", "CURSORUP", "ARROWUP", "NAVUP", "MENUUP", "DIRECTIONUP"),
    DOWN("DOWN", "CURSORDOWN", "ARROWDOWN", "NAVDOWN", "MENUDOWN", "DIRECTIONDOWN"),
    LEFT("LEFT", "CURSORLEFT", "ARROWLEFT", "NAVLEFT", "MENULEFT", "DIRECTIONLEFT"),
    RIGHT("RIGHT", "CURSORRIGHT", "ARROWRIGHT", "NAVRIGHT", "MENURIGHT", "DIRECTIONRIGHT"),
    OK("OK", "ENTER", "SELECT", "CURSORENTER", "CONFIRM", "MENUSELECT", "CURSOROK", "OKSELECT", "ENTEROK"),
    BACK("BACK", "RETURN", "LAST", "PRECH", "PREVCH", "PREVIOUSCHANNEL", "RECALL", "FLASHBACK"),
    EXIT("EXIT", "CANCEL", "QUIT", "MENUEXIT", "CLEAR"),
    MENU("MENU", "SETUP", "SETTINGS", "MAINMENU"),
    MUTE("MUTE", "MUTING", "SOUNDOFF", "AUDIOMUTE", "TVMUTE"),
    INPUT("INPUT", "SOURCE", "TVVIDEO", "TVAV", "AV", "INPUTSELECT", "INPUTSOURCE", "VIDEO", "AVTV", "TVINPUT", "INPUTSEL", "VIDEOINPUT", "TVSOURCE"),
    VOL_UP("VOLUMEUP", "VOLUMEPLUS", "VOLPLUS", "VOLUP", "VOLUMEINCREASE"),
    VOL_DOWN("VOLUMEDOWN", "VOLUMEMINUS", "VOLMINUS", "VOLDOWN", "VOLUMEDECREASE"),
    CH_UP("CHANNELUP", "CHANNELPLUS", "CHPLUS", "CHUP", "PROGPLUS", "PROGRAMPLUS", "PROGRAMUP", "PRPLUS", "PROGUP", "CHANUP"),
    CH_DOWN("CHANNELDOWN", "CHANNELMINUS", "CHMINUS", "CHDOWN", "PROGMINUS", "PROGRAMMINUS", "PROGRAMDOWN", "PRMINUS", "PROGDOWN", "CHANDOWN"),
    D0("0", "DIGIT0", "NUM0", "KP0"), D1("1", "DIGIT1", "NUM1", "KP1"), D2("2", "DIGIT2", "NUM2", "KP2"),
    D3("3", "DIGIT3", "NUM3", "KP3"), D4("4", "DIGIT4", "NUM4", "KP4"), D5("5", "DIGIT5", "NUM5", "KP5"),
    D6("6", "DIGIT6", "NUM6", "KP6"), D7("7", "DIGIT7", "NUM7", "KP7"), D8("8", "DIGIT8", "NUM8", "KP8"),
    D9("9", "DIGIT9", "NUM9", "KP9"),
    PLAY("PLAY", "PLAYPAUSE"), PAUSE("PAUSE", "PLAYPAUSE"), STOP("STOP"),
    REWIND("REWIND", "REW", "SCANMINUS", "FASTREVERSE"),
    FORWARD("FASTFORWARD", "FORWARD", "FF", "FFWD", "SCANPLUS"),
    RECORD("RECORD", "REC"),
    RED("RED"), GREEN("GREEN"), YELLOW("YELLOW"), BLUE("BLUE"),
    SLEEP("SLEEP", "SLEEPTIMER", "TIMER"),
    SUBTITLE("SUBTITLE", "SUBTITLES", "CC", "CAPTION");

    companion object {
        val digits = listOf(D1, D2, D3, D4, D5, D6, D7, D8, D9, D0)

        /** "KEY_VOLUMEUP", "Volume +", "VOL+" -> "VOLUMEUP", "VOLUMEPLUS", "VOLPLUS". */
        fun normalizeName(raw: String): String {
            var s = raw.uppercase().trim()
            if (s.startsWith("KEY_")) s = s.substring(4)
            s = s.replace("+", " PLUS ").replace(Regex("(?<=[A-Z ])-\\s*$"), " MINUS ")
            return s.filter { it.isLetterOrDigit() }
        }

        /** Escolhe a melhor função do modelo para a tecla, respeitando a ordem dos apelidos. */
        fun <T> pick(key: RemoteKey, functions: Map<String, T>): T? {
            for (a in key.aliases) functions[a]?.let { return it }
            return null
        }
    }
}
