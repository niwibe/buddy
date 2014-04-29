(ns buddy.test_buddy_core
  (:require [clojure.test :refer :all]
            [buddy.core.codecs :refer :all]
            [buddy.core.keys :refer :all]
            [buddy.core.hash :as hash]
            [buddy.core.hmac :refer [shmac-sha256]]
            [buddy.hashers.pbkdf2 :as pbkdf2]
            [buddy.hashers.bcrypt :as bcrypt]
            [buddy.hashers.sha256 :as sha256]
            [buddy.hashers.md5 :as md5]
            [buddy.hashers.scrypt :as scrypt]
            [buddy.core.mac.poly1305 :as poly]
            [clojure.java.io :as io])
  (:import buddy.Arrays))

(deftest buddy-core-codecs
  (testing "Hex encode/decode 01"
    (let [some-bytes  (str->bytes "FooBar")
          encoded     (bytes->hex some-bytes)
          decoded     (hex->bytes encoded)
          some-str    (bytes->str decoded)]
      (is (Arrays/equals decoded, some-bytes))
      (is (= some-str "FooBar"))))

  (testing "Hex encode/decode 02"
    (let [mybytes (into-array Byte/TYPE (range 10))
          encoded (bytes->hex mybytes)
          decoded (hex->bytes encoded)]
      (is (Arrays/equals decoded mybytes)))))

(deftest buddy-hashers
  (testing "Test low level api for encrypt/verify pbkdf2"
    (let [plain-password      "my-test-password"
          encrypted-password  (pbkdf2/make-password plain-password)]
      (is (pbkdf2/check-password plain-password encrypted-password))))

  (testing "Test low level api for encrypt/verify sha256"
    (let [plain-password      "my-test-password"
          encrypted-password  (sha256/make-password plain-password)]
      (is (sha256/check-password plain-password encrypted-password))))

  (testing "Test low level api for encrypt/verify md5"
    (let [plain-password      "my-test-password"
          encrypted-password  (md5/make-password plain-password)]
      (is (md5/check-password plain-password encrypted-password))))

  (testing "Test low level api for encrypt/verify bcrypt"
    (let [plain-password      "my-test-password"
          encrypted-password  (bcrypt/make-password plain-password)]
      (is (bcrypt/check-password plain-password encrypted-password))))

  (testing "Test low level api for encrypt/verify scrypt"
    (let [plain-password      "my-test-password"
          encrypted-password  (scrypt/make-password plain-password)]
      (is (scrypt/check-password plain-password encrypted-password)))))

(deftest buddy-core-hash
  (testing "SHA3 support test"
    (let [plain-text "FooBar"
          hashed     (-> (hash/sha3-256 plain-text)
                         (bytes->hex))]
      (is (= hashed "0a3c119a02a37e50fbaf8a3776559c76de7a969097c05bd0f41f60cf25210745"))))
  (testing "File hashing"
    (let [path       "test/_files/pubkey.ecdsa.pem"
          valid-hash "7aa01e35e65701c9a9d8f71c4cbf056acddc9be17fdff06b4c7af1b0b34ddc29"]
      (is (= (bytes->hex (hash/sha256 (io/input-stream path))) valid-hash)))))

(deftest core-mac-poly1305
  (let [iv        (byte-array 16) ;; 16 bytes array filled with 0
        plaintext "text"
        secretkey "secret"]
    (testing "Poly1305 encrypt/verify (using string key)"
      (let [mac-bytes1 (poly/poly1305 plaintext secretkey iv :aes)
            mac-bytes2 (poly/poly1305 plaintext secretkey iv :aes)]
      (is (= (Arrays/equals mac-bytes1 mac-bytes2)))))

  (testing "Poly1305 explicit encrypt/verify (using string key)"
    (let [mac-bytes1 (poly/poly1305 plaintext secretkey iv :aes)]
      (is (= (-> mac-bytes1 (bytes->hex)) "98a94ff88861bf9b96bcb7112b506579"))))

  (testing "Poly1305-AES enc/verify using key with good iv"
    (let [iv1      (make-random-bytes 16)
          iv2      (make-random-bytes 16)
          macbytes (poly/poly1305 plaintext secretkey iv1 :aes)]
      (is (poly/poly1305-verify plaintext macbytes secretkey iv1 :aes))
      (is (not (poly/poly1305-verify plaintext macbytes secretkey iv2 :aes)))))

  (testing "Poly1305-Twofish env/verify"
    (let [iv2 (make-random-bytes 16)
          signature (poly/poly1305-twofish plaintext secretkey iv2)]
      (is (poly/poly1305-twofish-verify plaintext signature secretkey iv2))
      (is (not (poly/poly1305-twofish-verify plaintext signature secretkey iv)))))

  (testing "Poly1305-Serpent env/verify"
    (let [iv2 (make-random-bytes 16)
          signature (poly/poly1305-serpent plaintext secretkey iv2)]
      (is (poly/poly1305-serpent-verify plaintext signature secretkey iv2))
      (is (not (poly/poly1305-serpent-verify plaintext signature secretkey iv)))))))

