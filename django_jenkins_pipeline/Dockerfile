FROM python:%python_version%
MAINTAINER Jonhnatha Trigueiro <joepreludian@gmail.com>

RUN apt-get update && apt-get install nginx supervisor python-pip -y && \
    pip install --upgrade pip gunicorn && pip2 install --upgrade supervisor supervisor-stdout && apt-get autoclean

RUN mkdir /app
ADD . /app

ADD nginx.conf /etc/nginx/nginx.conf
ADD supervisord.conf /etc/supervisord.conf
ADD entrypoint.sh /entrypoint.sh

WORKDIR /app

EXPOSE 80

CMD /entrypoint.sh


