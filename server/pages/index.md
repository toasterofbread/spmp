# SpMp Server (WIP) <a href="https://github.com/spectreseven1138/spmp"><spacer type="block" width="500" /><img src="images/GitHub-Mark-64px.png" width="24" height="24"></a>

## Authentication

For APIs that require it, an authentication key must be provided using the `key` url parameter

## API Reference

### Status
Provides basic status information
```
Key required: No
Path: /status
Return type: JSON
Return format: {
  "uptime": 0 # Time in seconds since the server started
}
```

### Stop
Stops the server after shutting down all components
```
Key required: Yes
Path: /stop
Return type: int
```

### Restart
Stops the server and starts it again
```
Key required: Yes
Path: /restart
Return type: int
```

### Update 
```
Key required: Yes
Path: /update
Return type: string
```