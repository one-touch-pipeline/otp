final Thread thread = Thread.getAllStackTraces().keySet().find {
  it.id == 1 && it.name == '...'
}
//thread.interrupt()
//thread.stop()  // Use only if thread does not respond to interrupt()
''
