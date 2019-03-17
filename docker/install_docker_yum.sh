echo "install unzip"
sudo yum install -y unzip
echo "remove old version docker"
sudo yum remove docker \
                  docker-common \
                  docker-selinux \
                  docker-engine

echo "install dependencies"
sudo yum install -y yum-utils \
  device-mapper-persistent-data \
  lvm2

echo "set up repo"
sudo yum-config-manager \
    --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo

echo "install the lastest docker-ce"
sudo yum install -y docker-ce

echo "start docker"
sudo systemctl start docker

echo "run hello-world"
sudo docker run hello-world
