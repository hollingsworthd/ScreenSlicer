### ScreenSlicer&#8482; -- automatic, zero-config web scraping&#8482;

**Licensed under the GNU Affero General Public License version 3.**

*Additional licensing permissions and support are available for paying customers. See LICENSE file for details.*

**Try a demo at** https://screenslicer.com/

ScreenSlicer is a web scraper. It requires no configuration, and it automatically queries search engines then extracts the results, optionally including the HTML at each result's URL. Using neural nets and tuned heuristics, ScreenSlicer is able to intelligently find a search box, enter a query, extract the results, and page forward in the results. In addition to keyword searches, structured form queries are supported too. And AJAX sites work just as well as static HTML ones.

Clustering is fully supported out of the box. Each request accepts IP addresses of ScreenSlicer instances (API docs linked below) and your requests will be balanced in a FIFO queue. Messages between nodes are encrypted with no need for SSL--just share a duplicate screenslicer.config file to each node (this file is auto-generated on first launch).

#### Installation instructions
https://github.com/MachinePublishers/ScreenSlicer/wiki/ScreenSlicer-Installation

#### API documentation
https://github.com/MachinePublishers/ScreenSlicer/wiki/ScreenSlicer-API
