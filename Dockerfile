# Dockerfile mejorado para Tomcat + driver PostgreSQL + espera por la BBDD
FROM tomcat:latest

# Copia del driver JDBC (ya lo teníais)
COPY postgresql-42.7.8.jar /usr/local/tomcat/lib/

# Instalamos netcat para poder comprobar puertos desde el contenedor
USER root
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -y --no-install-recommends netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

# Copiamos el script de arranque que espera a la BBDD
COPY start-tomcat.sh /usr/local/tomcat/bin/start-tomcat.sh
# Ensure script has Unix line endings and is executable
RUN sed -i 's/\r$//' /usr/local/tomcat/bin/start-tomcat.sh \
    && chmod +x /usr/local/tomcat/bin/start-tomcat.sh

# Volvemos al usuario por defecto (opcional, Tomcat ya funcionaría como root en muchas imágenes)
USER root

# Por defecto el script espera a las variables DB_HOST/DB_PORT y luego arranca Tomcat
ENTRYPOINT ["sh","/usr/local/tomcat/bin/start-tomcat.sh"]
