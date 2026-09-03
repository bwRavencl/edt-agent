<!-- markdownlint-disable-file line-length -->
# edt-agent

## 📖 Description

edt-agent is a Java Agent that helps catch [Swing](https://en.wikipedia.org/wiki/Swing_(Java)) thread-safety bugs at runtime.

Swing components are not thread-safe and are meant to be read from and written to only on the [Event Dispatch Thread](https://docs.oracle.com/javase/tutorial/uiswing/concurrency/dispatch.html) (EDT), but violations of this rule are easy to introduce by accident and often don't cause visible problems until much later, making them hard to track down after the fact.

This project is a modernized version of an approach described in Alexander Potochkin's blog post [Debugging Swing, the final summary](https://web.archive.org/web/20110319000457/http://weblogs.java.net/blog/2006/02/16/debugging-swing-final-summary), which used bytecode instrumentation to flag EDT violations as they happen.  
The original implementation relied on the third-party [ASM library](https://asm.ow2.io), while this version is built on top of the modern [Class-File API (JEP 484)](https://openjdk.org/jeps/484).

At a high level, edt-agent works by attaching as a `-javaagent` and registering a `ClassFileTransformer` that intercepts every `javax.swing.J*` class as it loads.  
For each getter, is accessor, or setter method on those classes, it rewrites the method's bytecode to insert a check at the very start of the method body: if `EventQueue.isDispatchThread()` returns `false`, it calls `Thread.dumpStack()` to print the current call stack to `stderr`.  
The original method body then runs unchanged. This means any code path that touches a Swing component's state off the EDT will immediately print a stack trace pointing at exactly where the violation occurred, with no changes needed to the instrumented application's own source code.

## 🛠️ Building

To build `edt-agent.jar`, run:

```sh
./gradlew jar
```

## ⚙️ Usage

To use the agent, run your application with the `-javaagent` option:

```sh
java -javaagent:/path/to/edt-agent.jar -jar your-app.jar
```

## ⚖️ License

[GNU General Public License v3.0](LICENSE)
