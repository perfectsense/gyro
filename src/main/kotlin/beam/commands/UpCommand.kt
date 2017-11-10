package beam.commands

import io.airlift.airline.Command
import java.io.File
import javax.script.ScriptEngineManager

@Command(name = "up")
class UpCommand : Runnable {

    override fun run() {
        println("up command running")
        val engineManager = ScriptEngineManager()
        val engine = engineManager.getEngineByName("kotlin")!!
        val config = File("cloud.beam").readText()
        println(config)
        engine.eval(config)
    }

}