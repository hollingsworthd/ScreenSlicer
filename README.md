### ScreenSlicer&#8482; -- automatic, zero-config web scraping&#8482;

**Licensed under the GNU Affero General Public License version 3.** In a nutshell, the [license](https://raw.githubusercontent.com/MachinePublishers/ScreenSlicer/master/LICENSE) means that all modified versions and linked subprograms (programs communicated with over APIs, HTTP, etc.) must disclose their entire source code to every user and that they must also be licensed as Affero GPLv3 or compatible.

*Additional licensing permissions (suitable for proprietary commercial development) and support are available for paying customers. See [LICENSE](https://raw.githubusercontent.com/MachinePublishers/ScreenSlicer/master/LICENSE) file for details.*

**Try a demo at** https://screenslicer.com/

**Download the latest release at** https://github.com/MachinePublishers/ScreenSlicer/releases/latest

**For paid licensing and consulting, email** ops@machinepublishers.com

ScreenSlicer is a web scraper. It requires no configuration, and it automatically queries search engines then extracts the results, optionally including the HTML at each result's URL. Using neural nets and tuned heuristics, ScreenSlicer is able to intelligently find a search box, enter a query, extract the results, and page forward in the results. In addition to keyword searches, structured form queries are supported too. And AJAX sites work just as well as static HTML ones.

Clustering is fully supported out of the box. Each request accepts IP addresses of ScreenSlicer servers (API docs linked below) and your requests will be balanced in a FIFO queue, with requests diverted away from busy servers. Messages between servers are encrypted with no need for SSL--just share a duplicate screenslicer.config file to each server (this file is auto-generated on first launch).

Proxying is supported and a proxy server can be specified on each request. SOCKS 5, SOCKS 4, HTTP, and SSL proxies can be used. The default proxy is a SOCKS 5 server running at 127.0.0.1:9050 (the standard connection for tor-socks which the installation instructions have you install). You can also use proxies which require username and password.

#### Installation instructions
https://github.com/MachinePublishers/ScreenSlicer/wiki/ScreenSlicer-Installation

#### API documentation
https://github.com/MachinePublishers/ScreenSlicer/wiki/ScreenSlicer-API
