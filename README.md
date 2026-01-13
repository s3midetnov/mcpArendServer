A sample program to make an MCP client to communicate with LLM's. 
The example task is typechecking with a programming language with dependent types Arend.

How to use it with Claude: run the Gradle task `installDist`, it will create a file in ./build/install/mcpArendServer/bin/ then in Claude desktop 
go to Settings -> Desktop app/Developer, click edit configuration and add the following (with the absolute path to the executable):
```json
{
    "mcpServers": {
        "arend": {
            "command" : "/Users/USERNAME/Dev/mcpArendServer/build/install/mcpArendServer/bin/mcpArendServer",
            "args" : []
        }
    }
}
```
Alternatively, to add it to JetBrains Junie, insert the same "arend" tool in Junie/Settings/MCP Settings.

The server is currently hardcoded to listen on port 9999. 
You need to run the typechecker server, wait until it typechecked the whole library, then you can use it with Claude or Junie.