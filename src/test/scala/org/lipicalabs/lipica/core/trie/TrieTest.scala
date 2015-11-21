package org.lipicalabs.lipica.core.trie

import java.nio.charset.StandardCharsets

import org.apache.commons.codec.binary.Hex
import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.db.datasource.HashMapDB
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, RBACCodec}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.collection.JavaConversions

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class TrieTest extends Specification {
	sequential

	val mockDb = new HashMapDB

	val LONG_STRING = "1234567890abcdefghijklmnopqrstuvwxxzABCEFGHIJKLMNOPQRSTUVWXYZ"
	val EMPTY_ROOT_HASH = RBACCodec.Encoder.encode(ImmutableBytes.empty).keccak256

	"empty key" should {
		"be right" in {

			val trie = new TrieImpl(mockDb)
			trie.update("", "dog")
			"dog" mustEqual new String(trie.get("").toByteArray)
		}
	}

	"simple case" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)
			trie.update("cat", "dog")
			"dog" mustEqual new String(trie.get("cat").toByteArray)
		}
	}

	"long string" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)
			trie.update("cat", LONG_STRING)
			LONG_STRING mustEqual new String(trie.get("cat").toByteArray)
		}
	}

	"multiple items (1)" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)

			trie.update("ca", "dude")
			new String(trie.get("ca").toByteArray) mustEqual "dude"

			trie.update("cat", "dog")
			new String(trie.get("cat").toByteArray) mustEqual "dog"

			trie.update("dog", "test")
			new String(trie.get("dog").toByteArray) mustEqual "test"

			trie.update("doge", LONG_STRING)
			new String(trie.get("doge").toByteArray) mustEqual LONG_STRING

			trie.update("test", LONG_STRING)
			new String(trie.get("test").toByteArray) mustEqual LONG_STRING

			//再確認。
			new String(trie.get("ca").toByteArray) mustEqual "dude"
			new String(trie.get("cat").toByteArray) mustEqual "dog"
			new String(trie.get("dog").toByteArray) mustEqual "test"
			new String(trie.get("doge").toByteArray) mustEqual LONG_STRING
			new String(trie.get("test").toByteArray) mustEqual LONG_STRING
		}
	}

	"multiple items (2)" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)

			trie.update("cat", "dog")
			new String(trie.get("cat").toByteArray) mustEqual "dog"

			trie.update("ca", "dude")
			new String(trie.get("ca").toByteArray) mustEqual "dude"

			trie.update("doge", LONG_STRING)
			new String(trie.get("doge").toByteArray) mustEqual LONG_STRING

			trie.update("dog", "test")
			new String(trie.get("dog").toByteArray) mustEqual "test"

			trie.update("test", LONG_STRING)
			new String(trie.get("test").toByteArray) mustEqual LONG_STRING

			//再確認。
			new String(trie.get("cat").toByteArray) mustEqual "dog"
			new String(trie.get("ca").toByteArray) mustEqual "dude"
			new String(trie.get("doge").toByteArray) mustEqual LONG_STRING
			new String(trie.get("dog").toByteArray) mustEqual "test"
			new String(trie.get("test").toByteArray) mustEqual LONG_STRING
		}
	}

	"short to short update" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)

			trie.update("cat", "dog")
			trie.get("cat").asString(StandardCharsets.UTF_8) mustEqual "dog"

			trie.update("cat", "dog1")
			trie.get("cat").asString(StandardCharsets.UTF_8) mustEqual "dog1"
		}
	}

	"long to long update" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)

			trie.update("cat", LONG_STRING)
			new String(trie.get("cat").toByteArray) mustEqual LONG_STRING

			trie.update("cat", LONG_STRING + "1")
			new String(trie.get("cat").toByteArray) mustEqual LONG_STRING + "1"
		}
	}

	"short to long update" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)

			trie.update("cat", "dog")
			new String(trie.get("cat").toByteArray) mustEqual "dog"

			trie.update("cat", LONG_STRING + "1")
			new String(trie.get("cat").toByteArray) mustEqual LONG_STRING + "1"
		}
	}

	"long to short update" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)

			trie.update("cat", LONG_STRING)
			new String(trie.get("cat").toByteArray) mustEqual LONG_STRING

			trie.update("cat", "dog1")
			new String(trie.get("cat").toByteArray) mustEqual "dog1"
		}
	}

	"delete short (1)" should {
		"be right" in {
			val ROOT_HASH_BEFORE = "a9539c810cc2e8fa20785bdd78ec36cc1dab4b41f0d531e80a5e5fd25c3037ee"
			val ROOT_HASH_AFTER = "fc5120b4a711bca1f5bb54769525b11b3fb9a8d6ac0b8bf08cbb248770521758"

			val trie = new TrieImpl(mockDb)

			trie.update("cat", "dog")
			new String(trie.get("cat").toByteArray) mustEqual "dog"

			trie.update("ca", "dude")
			new String(trie.get("ca").toByteArray) mustEqual "dude"
			trie.rootHash.toHexString mustEqual ROOT_HASH_BEFORE

			trie.delete("ca")
			new String(trie.get("ca").toByteArray) mustEqual ""
			trie.rootHash.toHexString mustEqual ROOT_HASH_AFTER
		}
	}

	"delete short (2)" should {
		"be right" in {
			val ROOT_HASH_BEFORE = "a9539c810cc2e8fa20785bdd78ec36cc1dab4b41f0d531e80a5e5fd25c3037ee"
			val ROOT_HASH_AFTER = "b25e1b5be78dbadf6c4e817c6d170bbb47e9916f8f6cc4607c5f3819ce98497b"

			val trie = new TrieImpl(mockDb)

			trie.update("ca", "dude")
			new String(trie.get("ca").toByteArray) mustEqual "dude"

			trie.update("cat", "dog")
			new String(trie.get("cat").toByteArray) mustEqual "dog"
			trie.rootHash.toHexString mustEqual ROOT_HASH_BEFORE

			trie.delete("cat")
			new String(trie.get("cat").toByteArray) mustEqual ""
			trie.rootHash.toHexString mustEqual ROOT_HASH_AFTER
		}
	}

	"delete short (3)" should {
		"be right" in {
			val ROOT_HASH_BEFORE = "778ab82a7e8236ea2ff7bb9cfa46688e7241c1fd445bf2941416881a6ee192eb"
			val ROOT_HASH_AFTER = "05875807b8f3e735188d2479add82f96dee4db5aff00dc63f07a7e27d0deab65"

			val trie = new TrieImpl(mockDb)

			trie.update("cat", "dude")
			new String(trie.get("cat").toByteArray) mustEqual "dude"

			trie.update("dog", "test")
			new String(trie.get("dog").toByteArray) mustEqual "test"
			trie.rootHash.toHexString mustEqual ROOT_HASH_BEFORE

			trie.delete("dog")
			new String(trie.get("dog").toByteArray) mustEqual ""
			trie.rootHash.toHexString mustEqual ROOT_HASH_AFTER
		}
	}

	"delete long (1)" should {
		"be right" in {
			val ROOT_HASH_BEFORE = "318961a1c8f3724286e8e80d312352f01450bc4892c165cc7614e1c2e5a0012a"
			val ROOT_HASH_AFTER = "63356ecf33b083e244122fca7a9b128cc7620d438d5d62e4f8b5168f1fb0527b"

			val trie = new TrieImpl(mockDb)

			trie.update("cat", LONG_STRING)
			new String(trie.get("cat").toByteArray) mustEqual LONG_STRING

			trie.update("dog", LONG_STRING)
			new String(trie.get("dog").toByteArray) mustEqual LONG_STRING
			trie.rootHash.toHexString mustEqual ROOT_HASH_BEFORE

			trie.delete("dog")
			new String(trie.get("dog").toByteArray) mustEqual ""
			trie.rootHash.toHexString mustEqual ROOT_HASH_AFTER
		}
	}

	"delete long (2)" should {
		"be right" in {
			val ROOT_HASH_BEFORE = "e020de34ca26f8d373ff2c0a8ac3a4cb9032bfa7a194c68330b7ac3584a1d388"
			val ROOT_HASH_AFTER = "334511f0c4897677b782d13a6fa1e58e18de6b24879d57ced430bad5ac831cb2"

			val trie = new TrieImpl(mockDb)

			trie.update("ca", LONG_STRING)
			new String(trie.get("ca").toByteArray) mustEqual LONG_STRING

			trie.update("cat", LONG_STRING)
			new String(trie.get("cat").toByteArray) mustEqual LONG_STRING
			trie.rootHash.toHexString mustEqual ROOT_HASH_BEFORE

			trie.delete("cat")
			new String(trie.get("cat").toByteArray) mustEqual ""
			trie.rootHash.toHexString mustEqual ROOT_HASH_AFTER
		}
	}

	"delete long (3)" should {
		"be right" in {
			val ROOT_HASH_BEFORE = "e020de34ca26f8d373ff2c0a8ac3a4cb9032bfa7a194c68330b7ac3584a1d388"
			val ROOT_HASH_AFTER = "63356ecf33b083e244122fca7a9b128cc7620d438d5d62e4f8b5168f1fb0527b"

			val trie = new TrieImpl(mockDb)

			trie.update("cat", LONG_STRING)
			new String(trie.get("cat").toByteArray) mustEqual LONG_STRING

			trie.update("ca", LONG_STRING)
			new String(trie.get("ca").toByteArray) mustEqual LONG_STRING
			trie.rootHash.toHexString mustEqual ROOT_HASH_BEFORE

			trie.delete("ca")
			new String(trie.get("ca").toByteArray) mustEqual ""
			trie.rootHash.toHexString mustEqual ROOT_HASH_AFTER
		}
	}

	"delete different items" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)
			val val1 = "2a"
			val val2 = "09"
			val val3 = "a9"

			trie.update(ImmutableBytes.parseHexString(val1), ImmutableBytes.parseHexString(val1))
			trie.update(ImmutableBytes.parseHexString(val2), ImmutableBytes.parseHexString(val2))
			val root1 = trie.rootHash.toHexString

			trie.update(ImmutableBytes.parseHexString(val3), ImmutableBytes.parseHexString(val3))
			trie.delete(ImmutableBytes.parseHexString(val3))
			val root2 = trie.rootHash.toHexString

			root1 mustEqual root2
		}
	}

	"delete multiple items (1)" should {
		"be right" in {
			val ROOT_HASH_BEFORE = "3a784eddf1936515f0313b073f99e3bd65c38689021d24855f62a9601ea41717"
			val ROOT_HASH_AFTER1 = "60a2e75cfa153c4af2783bd6cb48fd6bed84c6381bc2c8f02792c046b46c0653"
			val ROOT_HASH_AFTER2 = "a84739b4762ddf15e3acc4e6957e5ab2bbfaaef00fe9d436a7369c6f058ec90d"

			val trie = new TrieImpl(mockDb)

			trie.update("cat", "dog")
			new String(trie.get("cat").toByteArray) mustEqual "dog"

			trie.update("ca", "dude")
			new String(trie.get("ca").toByteArray) mustEqual "dude"

			trie.update("doge", LONG_STRING)
			new String(trie.get("doge").toByteArray) mustEqual LONG_STRING

			trie.update("dog", "test")
			new String(trie.get("dog").toByteArray) mustEqual "test"

			trie.update("test", LONG_STRING)
			new String(trie.get("test").toByteArray) mustEqual LONG_STRING

			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual ROOT_HASH_BEFORE

			val serialized = trie.serialize
			val newTrie = new TrieImpl(new HashMapDB)
			new String(newTrie.get("cat").toByteArray) mustEqual ""
			new String(newTrie.get("ca").toByteArray) mustEqual ""
			new String(newTrie.get("doge").toByteArray) mustEqual ""
			new String(newTrie.get("dog").toByteArray) mustEqual ""
			new String(newTrie.get("test").toByteArray) mustEqual ""
			newTrie.deserialize(serialized)
			new String(newTrie.get("cat").toByteArray) mustEqual "dog"
			new String(newTrie.get("ca").toByteArray) mustEqual "dude"
			new String(newTrie.get("doge").toByteArray) mustEqual LONG_STRING
			new String(newTrie.get("dog").toByteArray) mustEqual "test"
			new String(newTrie.get("test").toByteArray) mustEqual LONG_STRING

			trie.delete("dog")
			new String(trie.get("dog").toByteArray) mustEqual ""
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual ROOT_HASH_AFTER1

			trie.delete("test")
			new String(trie.get("test").toByteArray) mustEqual ""
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual ROOT_HASH_AFTER2
		}
	}

	"delete multiple items (2)" should {
		"be right" in {
			val ROOT_HASH_BEFORE = "cf1ed2b6c4b6558f70ef0ecf76bfbee96af785cb5d5e7bfc37f9804ad8d0fb56"
			val ROOT_HASH_AFTER1 = "f586af4a476ba853fca8cea1fbde27cd17d537d18f64269fe09b02aa7fe55a9e"
			val ROOT_HASH_AFTER2 = "c59fdc16a80b11cc2f7a8b107bb0c954c0d8059e49c760ec3660eea64053ac91"

			val trie = new TrieImpl(mockDb)

			trie.update("c", LONG_STRING)
			new String(trie.get("c").toByteArray) mustEqual LONG_STRING

			trie.update("ca", LONG_STRING)
			new String(trie.get("ca").toByteArray) mustEqual LONG_STRING

			trie.update("cat", LONG_STRING)
			new String(trie.get("cat").toByteArray) mustEqual LONG_STRING

			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual ROOT_HASH_BEFORE

			trie.delete("ca")
			new String(trie.get("ca").toByteArray) mustEqual ""
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual ROOT_HASH_AFTER1

			trie.delete("cat")
			new String(trie.get("cat").toByteArray) mustEqual ""
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual ROOT_HASH_AFTER2
		}
	}

	"delete all" should {
		"be right" in {
			val ROOT_HASH_BEFORE = "a84739b4762ddf15e3acc4e6957e5ab2bbfaaef00fe9d436a7369c6f058ec90d"

			val trie = new TrieImpl(mockDb)
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual EMPTY_ROOT_HASH.toHexString

			trie.update("ca", "dude")
			trie.update("cat", "dog")
			trie.update("doge", LONG_STRING)
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual ROOT_HASH_BEFORE

			trie.delete("ca")
			trie.delete("cat")
			trie.delete("doge")
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual EMPTY_ROOT_HASH.toHexString
		}
	}

	"values (1)" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)
			trie.update("A", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual "d23786fb4a010da3ce639d66d5e904a11dbc02746d1ce25029e53290cabf28ab"
		}
	}

	"values (2)" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)
			trie.update("doe", "reindeer")
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual "11a0327cfcc5b7689b6b6d727e1f5f8846c1137caaa9fc871ba31b7cce1b703e"

			trie.update("dog", "puppy")
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual "05ae693aac2107336a79309e0c60b24a7aac6aa3edecaef593921500d33c63c4"

			trie.update("dogglesworth", "cat")
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual "8aad789dff2f538bca5d8ea56e8abe10f4c7ba3a5dea95fea4cd6e7c3a1168d3"
		}
	}

	"values (3)" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)
			trie.update("do", "verb")
			trie.update("doge", "coin")
			trie.update("horse", "stallion")
			trie.update("dog", "puppy")
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual "5991bb8c6514148a29db676a14ac506cd2cd5775ace63c30a4fe457715e9ac84"
		}
	}

	"values (4)" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)
			trie.update("do", "verb")
			trie.update("ether", "wookiedoo")
			trie.update("horse", "stallion")
			trie.update("shaman", "horse")
			trie.update("doge", "coin")
			trie.update("ether", "")
			trie.update("dog", "puppy")
			trie.update("shaman", "")
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual "5991bb8c6514148a29db676a14ac506cd2cd5775ace63c30a4fe457715e9ac84"
		}
	}

	"values (5)" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)
			trie.update("foo", "bar")
			trie.update("food", "bat")
			trie.update("food", "bass")
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual "17beaa1648bafa633cda809c90c04af50fc8aed3cb40d16efbddee6fdf63c4c3"
		}
	}

	"values (6)" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)
			trie.update("be", "e")
			trie.update("dog", "puppy")
			trie.update("bed", "d")
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual "3f67c7a47520f79faa29255d2d3c084a7a6df0453116ed7232ff10277a8be68b"
		}
	}

	"values (7)" should {
		"be right" in {
			val trie = new TrieImpl(mockDb)
			trie.update("test", "test")
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual "85d106d4edff3b7a4889e91251d0a87d7c17a1dda648ebdba8c6060825be23b8"

			trie.update("te", "testy")
			Hex.encodeHexString(trie.rootHash.toByteArray) mustEqual "8452568af70d8d140f58d941338542f645fcca50094b20f3c3d8c3df49337928"
		}
	}


	"equals" should {
		"be right" in {
			val trie1 = new TrieImpl(mockDb)
			val trie2 = new TrieImpl(mockDb)

			trie1.update("dog", LONG_STRING)
			trie2.update("dog", LONG_STRING)
			trie1 mustEqual trie2

			trie1.update("dog", LONG_STRING)
			trie2.update("cat", LONG_STRING)
			trie1 mustNotEqual trie2
		}
	}

	"sync" should {
		"be right" in {
			val mockDb = new HashMapDB
			val trie = new TrieImpl(mockDb)
			trie.update("dog", LONG_STRING)
			mockDb.getAddedItems mustEqual 0

			trie.sync()
			mockDb.getAddedItems mustNotEqual 0
		}
	}

	"dirty tracking" should {
		"be right" in {
			val mockDb = new HashMapDB
			val trie = new TrieImpl(mockDb)
			trie.update("dog", LONG_STRING)
			trie.cache.isDirty mustEqual true

			trie.sync()
			trie.cache.isDirty mustEqual false

			trie.update("test", LONG_STRING)
			trie.cache.isDirty mustEqual true

			trie.cache.undo()
			trie.cache.isDirty mustEqual false
		}
	}

	"reset" should {
		"be right" in {
			val mockDb = new HashMapDB
			val trie = new TrieImpl(mockDb)
			trie.update("dog", LONG_STRING)
			trie.cache.getNodes.size mustNotEqual 0

			trie.cache.undo()
			trie.cache.getNodes.size mustEqual 0
		}
	}

	"undo" should {
		"be right" in {
			val mockDb = new HashMapDB
			val trie = new TrieImpl(mockDb)
			val dump0 = trie.dumpToString

			trie.update("doe", "reindeer")
			trie.sync()
			trie.rootHash.toHexString mustEqual "11a0327cfcc5b7689b6b6d727e1f5f8846c1137caaa9fc871ba31b7cce1b703e"
			val dump1 = trie.dumpToString
			dump1 mustNotEqual dump0

			trie.update("dog", "puppy")
			trie.rootHash.toHexString mustEqual "05ae693aac2107336a79309e0c60b24a7aac6aa3edecaef593921500d33c63c4"
			val dump2 = trie.dumpToString
			dump2 mustNotEqual dump1
			dump2 mustNotEqual dump0

			trie.undo()
			trie.rootHash.toHexString mustEqual "11a0327cfcc5b7689b6b6d727e1f5f8846c1137caaa9fc871ba31b7cce1b703e"
			val dump3 = trie.dumpToString
			dump3 mustEqual dump1
		}
	}

	"copy" should {
		"be right" in {
			val mockDb = new HashMapDB
			val trie1 = new TrieImpl(mockDb)
			trie1.update("doe", "reindeer")
			val trie2 = trie1.copy

			trie1.hashCode mustNotEqual trie2.hashCode
			trie1 mustEqual trie2
			trie1.rootHash.toHexString mustEqual trie2.rootHash.toHexString
		}
	}

	"fat trie" should {
		"be right" in {
			val mockDb = new HashMapDB
			val mockDb2 = new HashMapDB
			val fatTrie = new FatTrie(mockDb, mockDb2)

			val k1 = "do".getBytes
			val v1 = "verb".getBytes

			val k2 = "ether".getBytes
			val v2 = "wookiedoo".getBytes

			val k3 = "horse".getBytes
			val v3 = "stallion".getBytes

			val k4 = "shaman".getBytes
			val v4 = "horse".getBytes

			val k5 = "doge".getBytes
			val v5 = "coin".getBytes

			val k6 = "ether".getBytes
			val v6 = "".getBytes

			val k7 = "dog".getBytes
			val v7 = "puppy".getBytes

			val k8 = "shaman".getBytes
			val v8 = "".getBytes

			fatTrie.update(ImmutableBytes(k1), ImmutableBytes(v1))
			fatTrie.update(ImmutableBytes(k2), ImmutableBytes(v2))
			fatTrie.update(ImmutableBytes(k3), ImmutableBytes(v3))
			fatTrie.update(ImmutableBytes(k4), ImmutableBytes(v4))
			fatTrie.update(ImmutableBytes(k5), ImmutableBytes(v5))
			fatTrie.update(ImmutableBytes(k6), ImmutableBytes(v6))
			fatTrie.update(ImmutableBytes(k7), ImmutableBytes(v7))
			fatTrie.update(ImmutableBytes(k8), ImmutableBytes(v8))

			fatTrie.rootHash.toHexString mustEqual "29b235a58c3c25ab83010c327d5932bcf05324b7d6b1185e650798034783ca9d"
			fatTrie.secureTrie.rootHash.toHexString mustEqual "29b235a58c3c25ab83010c327d5932bcf05324b7d6b1185e650798034783ca9d"
			fatTrie.origTrie.rootHash.toHexString mustEqual "5991bb8c6514148a29db676a14ac506cd2cd5775ace63c30a4fe457715e9ac84"
		}
	}

	val randomDictionary = "spinneries, archipenko, prepotency, herniotomy, preexpress, relaxative, insolvably, debonnaire, apophysate, virtuality, cavalryman, utilizable, diagenesis, vitascopic, governessy, abranchial, cyanogenic, gratulated, signalment, predicable, subquality, crystalize, prosaicism, oenologist, repressive, impanelled, cockneyism, bordelaise, compigne, konstantin, predicated, unsublimed, hydrophane, phycomyces, capitalise, slippingly, untithable, unburnable, deoxidizer, misteacher, precorrect, disclaimer, solidified, neuraxitis, caravaning, betelgeuse, underprice, uninclosed, acrogynous, reirrigate, dazzlingly, chaffiness, corybantes, intumesced, intentness, superexert, abstrusely, astounding, pilgrimage, posttarsal, prayerless, nomologist, semibelted, frithstool, unstinging, ecalcarate, amputating, megascopic, graphalloy, platteland, adjacently, mingrelian, valentinus, appendical, unaccurate, coriaceous, waterworks, sympathize, doorkeeper, overguilty, flaggingly, admonitory, aeriferous, normocytic, parnellism, catafalque, odontiasis, apprentice, adulterous, mechanisma, wilderness, undivorced, reinterred, effleurage, pretrochal, phytogenic, swirlingly, herbarized, unresolved, classifier, diosmosing, microphage, consecrate, astarboard, predefying, predriving, lettergram, ungranular, overdozing, conferring, unfavorite, peacockish, coinciding, erythraeum, freeholder, zygophoric, imbitterer, centroidal, appendixes, grayfishes, enological, indiscreet, broadcloth, divulgated, anglophobe, stoopingly, bibliophil, laryngitis, separatist, estivating, bellarmine, greasiness, typhlology, xanthation, mortifying, endeavorer, aviatrices, unequalise, metastatic, leftwinger, apologizer, quatrefoil, nonfouling, bitartrate, outchiding, undeported, poussetted, haemolysis, asantehene, montgomery, unjoinable, cedarhurst, unfastener, nonvacuums, beauregard, animalized, polyphides, cannizzaro, gelatinoid, apologised, unscripted, tracheidal, subdiscoid, gravelling, variegated, interabang, inoperable, immortelle, laestrygon, duplicatus, proscience, deoxidised, manfulness, channelize, nondefense, ectomorphy, unimpelled, headwaiter, hexaemeric, derivation, prelexical, limitarian, nonionized, prorefugee, invariably, patronizer, paraplegia, redivision, occupative, unfaceable, hypomnesia, psalterium, doctorfish, gentlefolk, overrefine, heptastich, desirously, clarabelle, uneuphonic, autotelism, firewarden, timberjack, fumigation, drainpipes, spathulate, novelvelle, bicorporal, grisliness, unhesitant, supergiant, unpatented, womanpower, toastiness, multichord, paramnesia, undertrick, contrarily, neurogenic, gunmanship, settlement, brookville, gradualism, unossified, villanovan, ecospecies, organising, buckhannon, prefulfill, johnsonese, unforegone, unwrathful, dunderhead, erceldoune, unwadeable, refunction, understuff, swaggering, freckliest, telemachus, groundsill, outslidden, bolsheviks, recognizer, hemangioma, tarantella, muhammedan, talebearer, relocation, preemption, chachalaca, septuagint, ubiquitous, plexiglass, humoresque, biliverdin, tetraploid, capitoline, summerwood, undilating, undetested, meningitic, petrolatum, phytotoxic, adiphenine, flashlight, protectory, inwreathed, rawishness, tendrillar, hastefully, bananaquit, anarthrous, unbedimmed, herborized, decenniums, deprecated, karyotypic, squalidity, pomiferous, petroglyph, actinomere, peninsular, trigonally, androgenic, resistance, unassuming, frithstool, documental, eunuchised, interphone, thymbraeus, confirmand, expurgated, vegetation, myographic, plasmagene, spindrying, unlackeyed, foreknower, mythically, albescence, rebudgeted, implicitly, unmonastic, torricelli, mortarless, labialized, phenacaine, radiometry, sluggishly, understood, wiretapper, jacobitely, unbetrayed, stadholder, directress, emissaries, corelation, sensualize, uncurbable, permillage, tentacular, thriftless, demoralize, preimagine, iconoclast, acrobatism, firewarden, transpired, bluethroat, wanderjahr, groundable, pedestrian, unulcerous, preearthly, freelanced, sculleries, avengingly, visigothic, preharmony, bressummer, acceptable, unfoolable, predivider, overseeing, arcosolium, piriformis, needlecord, homebodies, sulphation, phantasmic, unsensible, unpackaged, isopiestic, cytophagic, butterlike, frizzliest, winklehawk, necrophile, mesothorax, cuchulainn, unrentable, untangible, unshifting, unfeasible, poetastric, extermined, gaillardia, nonpendent, harborside, pigsticker, infanthood, underrower, easterling, jockeyship, housebreak, horologium, undepicted, dysacousma, incurrable, editorship, unrelented, peritricha, interchaff, frothiness, underplant, proafrican, squareness, enigmatise, reconciled, nonnumeral, nonevident, hamantasch, victualing, watercolor, schrdinger, understand, butlerlike, hemiglobin, yankeeland"
	"massive update" should {
		"be right" in {
			val randomWords = randomDictionary.split(",").map(_.trim).toIndexedSeq
			println("Random words = %,d".format(randomWords.size))
			val testerMap = JavaConversions.mapAsScalaMap(new java.util.TreeMap[String, String])
			val mockDb = new HashMapDB
			val trie = new TrieImpl(mockDb)

			val seed = System.currentTimeMillis
			val generator = new java.util.Random(seed)
			println("Seed of RNG is: %,d".format(seed))
			(0 until 100).foreach {
				i => {
					val randomIndex1 = generator.nextInt(randomWords.size)
					val word1 = randomWords(randomIndex1)
					//println(randomIndex1 + " -> " + word1)
					val word2 = randomWords(generator.nextInt(randomWords.size))
					trie.update(word1, word2)
					testerMap.put(word1, word2)

					val retrieved = trie.get(word1).asString(StandardCharsets.UTF_8)
					retrieved mustEqual word2
					//println("Updated: [%d] %s -> %s".format(i, word1, retrieved))
				}
			}
			val half = testerMap.size / 2
			(0 until half).foreach {
				i => {
					val word1 = randomWords(generator.nextInt(randomWords.size))
					trie.delete(word1)
					testerMap.remove(word1)

					val retrieved = trie.get(word1).asString(StandardCharsets.UTF_8)
					retrieved mustEqual ""
					//println("Deleted: [%d] %s".format(i, word1))
				}
			}

			trie.cleanCache()
			val prev = mockDb.getAddedItems
			trie.sync()
			println("Committed: %d -> %d".format(prev, mockDb.getAddedItems))

			var i = 0
			testerMap.foreach {
				entry => {
					//println("[%d] %s -> %s".format(i, entry._1, entry._2))
					val trieWord2 = trie.get(entry._1).asString(StandardCharsets.UTF_8)
					trieWord2 mustEqual entry._2
					i += 1
				}
			}
			ok
		}
	}
}
