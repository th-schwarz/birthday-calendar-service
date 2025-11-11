# Build &amp; Push

docker build -t thschwarz/radicale-it:1.0 .

docker buildx build -–platform linux/amd64 -t thschwarz/radicale-it:1.0 -–push -f Dockerfile .