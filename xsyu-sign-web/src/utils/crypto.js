import JSEncrypt from 'jsencrypt'

// RSA 公钥加密（用于登录密码传输，与后端 RSAUtils 对应）
export function rsaEncrypt(text, publicKey) {
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(publicKey)
  return encrypt.encrypt(text)
}
