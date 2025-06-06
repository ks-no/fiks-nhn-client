package no.ks.fiks.nhn


fun readResourceContentAsString(path: String) = readResourceContent(path).decodeToString()

fun readResourceContent(path: String) = ClassLoader.getSystemResource(path).readBytes()
