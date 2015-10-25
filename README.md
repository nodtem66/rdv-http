# RDV-HTTP
An RDV-like (real-time data viewer) HTTP server for [DataTurbine](http://dataturbine.org) sink application

## Actors
* `Root Actor` binds the HTTP server 
* `API Actor` handles url route via _spray_ library
    * `Respond Actor` prepares the returned messages to JSON-formatted string
* `Connection Supervisor Actor` control the creation, restart, and stop `Connection Actor`
    * `Connection Actor` query the endpoint info from [DataTurbine](http://dataturbine.org) server from via DSN query string
* `Session Supervisor Actor` control the creation, restart, and stop `Session Actor`
    * `Session Actor` stream and buffer data from [DataTurbine](http://dataturbine.org) and return the recent buffer data to `API Actor`
    
## TODO
* Implement `session supervisor actor and sessoin actor`
:bowtie: