# Contributing to LiTr

LiTr is designed to encourage contribution. Transformation process is divided into five distinct steps, and components for each step are pluggable:
 - Overriding `MediaSource` allows reading data from sources Android's MediaExtractor can't, such as stream from camera or unsupported container formats
 - Implementing custom `Decoder` and/or `Encoder` allows experimentation with not yet supported codecs (for example, AV1)
 - Overriding `MediaTarget` allows writing data using custom muxer (MKV, for instance)
 - Custom `Renderer` can do things beyond simple resizing - ML based frame modification, audio mixing, etc.
 
In addition, it should be quite easy to develop and contribute new filters by implementing `GlFilter` interface. Please contribute filters into `litr-filters` library.

## Contribution Agreement

As a contributor, you represent that the code you submit is your original work or that of your employer
(in which case you represent you have the right to bind your employer). By submitting code, you
(and, if applicable, your employer) are licensing the submitted code to LinkedIn and the open source
community subject to the BSD 2-Clause license.

## Responsible Disclosure of Security Vulnerabilities

Please do not file reports on Github for security issues. Please send vulnerability reports to
security@linkedin.com preferably with the title "Github linkedin/ - ".

## Tips for Getting Your Pull Request Accepted

- Make sure all new features are tested and the tests pass.
- Bug fixes must include a test case demonstrating the error that it fixes.
- Open an issue first and seek advice for your change before submitting a pull request. Large features which have never been discussed are unlikely to be accepted. You have been warned.