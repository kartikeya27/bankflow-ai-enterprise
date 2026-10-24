FROM python:3.12-alpine
WORKDIR /srv
COPY server.py .
RUN adduser -D app
USER app
EXPOSE 8090
CMD ["python","server.py"]
