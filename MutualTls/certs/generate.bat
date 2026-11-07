@echo off
rem ---------------------------------------------------------------------------
rem  Генерация набора сертификатов для mTLS на Windows: CA, сервер, клиент.
rem  Запуск:  cd MutualTls\certs, затем generate.bat   (openssl должен быть в PATH)
rem ---------------------------------------------------------------------------
chcp 65001 > nul
setlocal
cd /d "%~dp0"

echo ==^> 1/3  Root CA
openssl genpkey -quiet -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out ca.key || goto :error
openssl req -new -x509 -utf8 -config ca.cnf -key ca.key -days 1095 -out ca.pem || goto :error

for %%N in (server client) do (
  echo ==^> %%N
  openssl genpkey -quiet -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out %%N.key || goto :error
  openssl req -new -utf8 -config %%N.cnf -key %%N.key -out %%N.csr || goto :error
  openssl x509 -req -in %%N.csr -CA ca.pem -CAkey ca.key -CAcreateserial -days 825 -extfile %%N.cnf -extensions %%N_ext -out %%N.pem || goto :error
  del %%N.csr
)

echo ==^> Verify
openssl verify -CAfile ca.pem server.pem client.pem || goto :error
echo ==^> Done
exit /b 0

:error
echo Ошибка генерации сертификатов
exit /b 1
