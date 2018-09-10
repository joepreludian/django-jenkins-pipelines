FROM python:%python_version%
MAINTAINER Jonhnatha Trigueiro <joepreludian@gmail.com>

RUN apt-get update && apt-get install nginx supervisor python-pip -y && \
    pip install --upgrade pip && pip2 install --upgrade supervisor supervisor-stdout && apt-get autoclean

ADD entrypoint.sh /entrypoint.sh

CMD /entrypoint.sh
