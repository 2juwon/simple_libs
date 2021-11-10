package devdan.libs.base.widgets

abstract class ThreadTask<T1, T2> : Runnable {
    // Argument
    private var _argument: T1? = null

    // Result
    private var _result: T2? = null

    private var _thread: Thread? = null

    // Execute
    fun execute(arg: T1) {
        // Store the argument
        _argument = arg

        // Call onPreExecute
        onPreExecute()

        // Begin thread work
        _thread = Thread(this)
        _thread?.start()

        // Wait for the thread work
        try {
            _thread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
            onPostExecute(null)
            return
        }

        // Call onPostExecute
        onPostExecute(_result)
        _thread = null
    }

    override fun run() {
        _result = doInBackground(_argument)
    }

    fun stop() {
        _thread?.interrupt()
    }

    // onPreExecute
    protected abstract fun onPreExecute()

    // doInBackground
    protected abstract fun doInBackground(arg: T1?): T2?

    // onPostExecute
    protected abstract fun onPostExecute(result: T2?)
}