import org.codehaus.groovy.control.CompilerConfiguration

class GroovyEval {
    static void main(String[] args) {
        def configuration = new CompilerConfiguration()
        def shell = new GroovyShell(this.class.classLoader, new Binding(), configuration)
        def result = shell.evaluate(args[0])
        println(result)
    }
}