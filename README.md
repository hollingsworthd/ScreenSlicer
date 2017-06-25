ScreenSlicer will be re-worked to be a library easily incorporated into other applications. No maintenance will be done to the codebase in its current form.

![dandelion in the wind](https://user-images.githubusercontent.com/5241490/27517334-c70bf2a0-5998-11e7-83da-66c3706212f8.jpg)

Licensed under the Apache License v2.0 ([details](https://raw.githubusercontent.com/MachinePublishers/ScreenSlicer/master/LICENSE)).

- - -

[Download](https://github.com/MachinePublishers/ScreenSlicer/releases/latest) | [Getting Started](https://github.com/MachinePublishers/ScreenSlicer/wiki/ScreenSlicer-Installation) | [API Docs](https://github.com/MachinePublishers/ScreenSlicer/wiki/ScreenSlicer-API) | [Build Guide](https://github.com/MachinePublishers/ScreenSlicer/wiki/Building-ScreenSlicer) | [Also see: jBrowserDriver](https://github.com/MachinePublishers/jBrowserDriver)

- - -

### Overview

ScreenSlicer is a web scraper. It requires no configuration, and it automatically queries search engines then extracts the results, optionally including the HTML at each result's URL. Using neural nets and tuned heuristics, ScreenSlicer is able to intelligently find a search box, enter a query, extract the results, and page forward in the results. In addition to keyword searches, structured form queries are supported too. And AJAX sites work just as well as static HTML ones. Sites with authentication (username/password) are also supported.

Clustering is built in. Each request accepts IP addresses of ScreenSlicer servers (see API docs linked below) and your requests will be balanced in a queue, with requests diverted away from busy servers. Messages between servers are encrypted with no need for SSL--just share a duplicate screenslicer.config file with each installation (this file is auto-generated at the root of the installation directory on first launch).

Proxying is supported and a proxy server can be specified on each request. SOCKS 5, SOCKS 4, HTTP, and SSL proxies can be used. The default proxy is a SOCKS 5 server running at 127.0.0.1:9050 (the standard connection for tor-socks which the installation instructions have you install). You can also use proxies which require username and password.

- - -

Copyright (C) 2013-2015 ScreenSlicer committers
