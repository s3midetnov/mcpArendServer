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

You need to run the typechecker server, wait until it typechecked the whole library, then you can use it with Claude or Junie. 
You can run the typechecker server either in IDE or with `/Users/USERNAME/Dev/mcpArendServer/typecheckerServer/build/install/mcpArendServer/bin/typecheckerServer`. 
## Usage

Run the server application from the console. You can override the default configuration using the following command-line arguments:

| Flag | Long Flag    | Description                                      | Default       |
| :--- | :----------- | :----------------------------------------------- | :------------ |
| `-p` | `--port`     | The port number the server listens on.           | `9999`        |
| `-t` | `--timeout`  | Server lifetime in seconds before auto-shutdown. | `1800` (30m)  |
| `-l` | `--lib`      | Path to the Arend library used for typechecking. | *[System Default]* |
| `-h` | `--help`     | Show the help message and exit.                  | N/A           |

### Examples

**Run with defaults:**
```bash
java -jar build/libs/server.jar