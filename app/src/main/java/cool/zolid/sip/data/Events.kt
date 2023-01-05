package cool.zolid.sip.data

data class HistoryChangeEvent(val data: Map<String, Map<String, String>>)
data class AllClassesChangeEvent(val data: List<String>)