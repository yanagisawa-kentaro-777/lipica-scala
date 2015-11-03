package org.lipicalabs.lipica.core.vm

import org.lipicalabs.lipica.core.utils.LipicaMiscUtils
import org.lipicalabs.lipica.core.vm.Tier._


/**
 * Lipica VM の命令が属するティア。
 *
 * @since 2015/10/17
 * @author YANAGISAWA, Kentaro
 */
sealed trait Tier {
	def level: Int
	def asInt: Int = this.level
	def name: String = "%s (Level %d)".format(getClass.getSimpleName, level)
	override def toString: String = this.name
}
object Tier {
	val all: Set[Tier] = {
		LipicaMiscUtils.sealedDescendants[Tier].getOrElse(Set.empty).asInstanceOf[Set[Tier]]
	}

	object ZeroTier extends Tier {
		override val level = 0
	}
	object BaseTier extends Tier {
		override val level = 2
	}
	object VeryLowTier extends Tier {
		override val level = 3
	}
	object LowTier extends Tier {
		override val level = 5
	}
	object MidTier extends Tier {
		override val level = 6
	}
	object HighTier extends Tier {
		override val level = 10
	}
	object ExtTier extends Tier {
		override val level = 20
	}
	object SpecialTier extends Tier {
		override val level = 1
	}
	object InvalidTier extends Tier {
		override val level = 0
	}
}



/**
 * Lipica VM の命令セット。
 *
 * @since 2015/10/17
 * @author YANAGISAWA, Kentaro
 */
sealed class OpCode(val opcode: Byte, val require: Int, val ret: Int, val tier: Tier) {
	def name: String = "%s (%d)".format(getClass.getSimpleName, opcode)
}


object OpCode {

	/**
	 * 0x00：停止。
	 */
	object Stop extends OpCode(0x00, 0, 0, ZeroTier)

	//以下、算術演算。
	/**
	 * 0x01：加算。
	 */
	object Add extends OpCode(0x01, 2, 1, VeryLowTier)

	/**
	 * 0x02：乗算。
	 */
	object Mul extends OpCode(0x02, 2, 1, LowTier)

	/**
	 * 0x03：減算。
	 */
	object Sub extends OpCode(0x03, 2, 1, VeryLowTier)

	/**
	 * 0x04：整数除算。
	 */
	object Div extends OpCode(0x04, 2, 1, LowTier)

	/**
	 * 0x05：符号付き整数除算。
	 */
	object SDiv extends OpCode(0x05, 2, 1, LowTier)

	/**
	 * 0x06：剰余演算。
	 */
	object Mod extends OpCode(0x06, 2, 1, LowTier)

	/**
	 * 0x07：符号付き剰余演算。
	 */
	object SMod extends OpCode(0x07, 2, 1, LowTier)

	/**
	 * 0x08：加算と剰余演算との組み合わせ。
	 */
	object AddMod extends OpCode(0x08, 3, 1, MidTier)

	/**
	 * 0x09：乗算と剰余演算との組み合わせ。
	 */
	object MulMod extends OpCode(0x09, 3, 1, MidTier)

	/**
	 * 0x0a：べき乗。
	 */
	object Exp extends OpCode(0x0a, 2, 1, SpecialTier)

	/**
	 * 0x0b：符号付き整数において、符号部分のバイト数を下位ビットに拡張する。
	 */
	object SignExtend extends OpCode(0x0b, 2, 1, LowTier)

	//以下、比較／論理演算。
	/**
	 * 0x10：未満。
	 */
	object Lt extends OpCode(0x10, 2, 1, VeryLowTier)

	/**
	 * 0x11：超。
	 */
	object Gt extends OpCode(0x11, 2, 1, VeryLowTier)

	/**
	 * 0x12：符号付きLT。
	 */
	object SLt extends OpCode(0x12, 2, 1, VeryLowTier)

	/**
	 * 0x13：符号付きGT。
	 */
	object SGt extends OpCode(0x13, 2, 1, VeryLowTier)

	/**
	 * 0x14：等値。
	 */
	object Eq extends OpCode(0x14, 2, 1, VeryLowTier)

	/**
	 * 0x15：命題論理における否定。
	 */
	object IsZero extends OpCode(0x15, 1, 1, VeryLowTier)

	/**
	 * 0x16：ビットごとの論理積。
	 */
	object And extends OpCode(0x16, 2, 1, VeryLowTier)

	/**
	 * 0x17：ビットごとの論理和。
	 */
	object Or extends OpCode(0x17, 2, 1, VeryLowTier)

	/**
	 * 0x18：ビットごとのXOR。
	 */
	object Xor extends OpCode(0x18, 2, 1, VeryLowTier)

	/**
	 * 0x19：ビットごとの反転。
	 */
	object Not extends OpCode(0x19, 1, 1, VeryLowTier)

	/**
	 * 0x1a：Wordから添字によって１バイトを読み取る。
	 */
	object Byte extends OpCode(0x1a, 2, 1, VeryLowTier)

	//暗号学的操作。

	/**
	 * 0x20：SHA3-256ダイジェスト値を計算する。
	 */
	object SHA3 extends OpCode(0x20, 2, 1, SpecialTier)

	//環境情報。

	/**
	 * 0x30：現在実行中のアカウントのアドレスを取得する。
	 */
	object Address extends OpCode(0x30, 0, 1, BaseTier)

	/**
	 * 0x31：渡されたアカウントの現在の残高を取得する。
	 */
	object Balance extends OpCode(0x31, 1, 1, ExtTier)

	/**
	 * 0x32：実行を開始したアドレスを取得する。
	 */
	object Origin extends OpCode(0x32, 0, 1, BaseTier)

	/**
	 * 0x33：呼び出し元アドレスを取得する。
	 */
	object Caller extends OpCode(0x33, 0, 1, BaseTier)

	/**
	 * 0x34：この実行に責任がある処理によって入金された額を取得する。
	 */
	object CallValue extends OpCode(0x34, 0, 1, BaseTier)

	/**
	 * 0x35：現在の環境における入力データを取得する。
	 */
	object CallDataLoad extends OpCode(0x35, 1, 1, VeryLowTier)

	/**
	 * 0x36：現在の環境における入力データの長さを取得する。
	 */
	object CallDataSize extends OpCode(0x36, 0, 1, VeryLowTier)

	/**
	 * 0x37：現在の環境における入力データをメモリにコピーする。
	 */
	object CallDataCopy extends OpCode(0x37, 3, 0, VeryLowTier)

	/**
	 * 0x38：現在の環境で動作しているコードの長さを取得する。
	 */
	object CodeSize extends OpCode(0x38, 0, 1, BaseTier)

	/**
	 * 0x39：現在の環境で動作しているコードをメモリにコピーする。
	 */
	object CodeCopy extends OpCode(0x39, 3, 0, VeryLowTier)

	/**
	 * 0x3a：１マナの価格を返します。
	 */
	object ManaPrice extends OpCode(0x3a, 0, 1, VeryLowTier)

	/**
	 * 0x3b：現在の環境で動作しているコードのオフセット以後の長さを取得する。
	 */
	object ExtCodeSize extends OpCode(0x3b, 1, 1, ExtTier)

	/**
	 * 0x3c：現在の環境で動作しているコードのオフセット以後の部分をメモリにコピーする。
	 */
	object ExtCodeCopy extends OpCode(0x3c, 4, 0, ExtTier)

	//ブロック情報。

	/**
	 * 0x40：最近の完全なブロックのハッシュ値を取得する。
	 */
	object BlockHash extends OpCode(0x40, 1, 1, ExtTier)

	/**
	 * 0x41：ブロックのコインベースアドレスを取得する。
	 */
	object Coinbase extends OpCode(0x41, 0, 1, BaseTier)

	/**
	 * 0x42：ブロックのタイムスタンプを取得する。
	 */
	object Timestamp extends OpCode(0x42, 0, 1, BaseTier)

	/**
	 * 0x43：ブロックの番号を取得する。
	 */
	object Number extends OpCode(0x43, 0, 1, BaseTier)

	/**
	 * 0x44：ブロックの難度を取得する。
	 */
	object Difficulty extends OpCode(0x44, 0, 1, BaseTier)

	/**
	 * 0x45：ブロックのマナ上限を取得する。
	 */
	object ManaLimit extends OpCode(0x45, 0, 1, BaseTier)

	//メモリ、ストレージ、制御に関する操作。

	/**
	 * 0x50：スタックから要素を取り出す。
	 */
	object Pop extends OpCode(0x50, 1, 0, BaseTier)

	/**
	 * 0x51：メモリから１ワードを読み取る。
	 */
	object MLoad extends OpCode(0x51, 1, 1, VeryLowTier)

	/**
	 * 0x52：メモリに１ワードを書き込む。
	 */
	object MStore extends OpCode(0x52, 2, 0, VeryLowTier)

	/**
	 * 0x53：メモリに１バイトを書き込む。
	 */
	object MStore8 extends OpCode(0x53, 2, 0, VeryLowTier)

	/**
	 * 0x54：ストレージから１ワードを読み取る。
	 */
	object SLoad extends OpCode(0x54, 1, 1, SpecialTier)

	/**
	 * 0x55：ストレージに１ワードを記録する。
	 */
	object SStore extends OpCode(0x55, 2, 0, SpecialTier)

	/**
	 * 0x56：プログラムカウンタを変更する。
	 */
	object Jump extends OpCode(0x56, 1, 0, MidTier)

	/**
	 * 0x57：条件に応じて、プログラムカウンタを変更する。
	 */
	object JumpI extends OpCode(0x57, 2, 0, HighTier)

	/**
	 * 0x58：プログラムカウンタを取得する。
	 */
	object PC extends OpCode(0x58, 0, 1, BaseTier)

	/**
	 * 0x59：アクティブメモリの容量を取得する。
	 */
	object MSize extends OpCode(0x59, 0, 1, BaseTier)

	/**
	 * 0x58：利用できるマナの量を取得する。
	 */
	object Mana extends OpCode(0x5a, 0, 1, BaseTier)

	/**
	 * 0x5b：
	 */
	object JumpDest extends OpCode(0x5b, 0, 0, SpecialTier)

	//プッシュ操作。
	//1バイト～32バイトのデータをスタックにプッシュする。
	object Push1 extends OpCode(0x60, 0, 1, VeryLowTier)
	object Push2 extends OpCode(0x61, 0, 1, VeryLowTier)
	object Push3 extends OpCode(0x62, 0, 1, VeryLowTier)
	object Push4 extends OpCode(0x63, 0, 1, VeryLowTier)
	object Push5 extends OpCode(0x64, 0, 1, VeryLowTier)
	object Push6 extends OpCode(0x65, 0, 1, VeryLowTier)
	object Push7 extends OpCode(0x66, 0, 1, VeryLowTier)
	object Push8 extends OpCode(0x67, 0, 1, VeryLowTier)
	object Push9 extends OpCode(0x68, 0, 1, VeryLowTier)
	object Push10 extends OpCode(0x69, 0, 1, VeryLowTier)
	object Push11 extends OpCode(0x6a, 0, 1, VeryLowTier)
	object Push12 extends OpCode(0x6b, 0, 1, VeryLowTier)
	object Push13 extends OpCode(0x6c, 0, 1, VeryLowTier)
	object Push14 extends OpCode(0x6d, 0, 1, VeryLowTier)
	object Push15 extends OpCode(0x6e, 0, 1, VeryLowTier)
	object Push16 extends OpCode(0x6f, 0, 1, VeryLowTier)
	object Push17 extends OpCode(0x70, 0, 1, VeryLowTier)
	object Push18 extends OpCode(0x71, 0, 1, VeryLowTier)
	object Push19 extends OpCode(0x72, 0, 1, VeryLowTier)
	object Push20 extends OpCode(0x73, 0, 1, VeryLowTier)
	object Push21 extends OpCode(0x74, 0, 1, VeryLowTier)
	object Push22 extends OpCode(0x75, 0, 1, VeryLowTier)
	object Push23 extends OpCode(0x76, 0, 1, VeryLowTier)
	object Push24 extends OpCode(0x77, 0, 1, VeryLowTier)
	object Push25 extends OpCode(0x78, 0, 1, VeryLowTier)
	object Push26 extends OpCode(0x79, 0, 1, VeryLowTier)
	object Push27 extends OpCode(0x7a, 0, 1, VeryLowTier)
	object Push28 extends OpCode(0x7b, 0, 1, VeryLowTier)
	object Push29 extends OpCode(0x7c, 0, 1, VeryLowTier)
	object Push30 extends OpCode(0x7d, 0, 1, VeryLowTier)
	object Push31 extends OpCode(0x7e, 0, 1, VeryLowTier)
	object Push32 extends OpCode(0x7f, 0, 1, VeryLowTier)

	//スタックの1番目～16番目のデータを複製する。
	object Dup1 extends OpCode(0x81.toByte, 1, 2, VeryLowTier)
	object Dup2 extends OpCode(0x82.toByte, 2, 3, VeryLowTier)
	object Dup3 extends OpCode(0x83.toByte, 3, 4, VeryLowTier)
	object Dup4 extends OpCode(0x84.toByte, 4, 5, VeryLowTier)
	object Dup5 extends OpCode(0x85.toByte, 5, 6, VeryLowTier)
	object Dup6 extends OpCode(0x86.toByte, 6, 7, VeryLowTier)
	object Dup7 extends OpCode(0x87.toByte, 7, 8, VeryLowTier)
	object Dup8 extends OpCode(0x88.toByte, 8, 9, VeryLowTier)
	object Dup9 extends OpCode(0x89.toByte, 9, 10, VeryLowTier)
	object Dup10 extends OpCode(0x90.toByte, 10, 11, VeryLowTier)
	object Dup11 extends OpCode(0x91.toByte, 11, 12, VeryLowTier)
	object Dup12 extends OpCode(0x92.toByte, 12, 13, VeryLowTier)
	object Dup13 extends OpCode(0x93.toByte, 13, 14, VeryLowTier)
	object Dup14 extends OpCode(0x94.toByte, 14, 15, VeryLowTier)
	object Dup15 extends OpCode(0x95.toByte, 15, 16, VeryLowTier)
	object Dup16 extends OpCode(0x96.toByte, 16, 17, VeryLowTier)

	//スタックの2番目～17番目のデータを、1番目のデータと交換する。
	object Swap1 extends OpCode(0x90.toByte, 2, 2, VeryLowTier)
	object Swap2 extends OpCode(0x91.toByte, 3, 3, VeryLowTier)
	object Swap3 extends OpCode(0x92.toByte, 4, 4, VeryLowTier)
	object Swap4 extends OpCode(0x93.toByte, 5, 5, VeryLowTier)
	object Swap5 extends OpCode(0x94.toByte, 6, 6, VeryLowTier)
	object Swap6 extends OpCode(0x95.toByte, 7, 7, VeryLowTier)
	object Swap7 extends OpCode(0x96.toByte, 8, 8, VeryLowTier)
	object Swap8 extends OpCode(0x97.toByte, 9, 9, VeryLowTier)
	object Swap9 extends OpCode(0x98.toByte, 10, 10, VeryLowTier)
	object Swap10 extends OpCode(0x99.toByte, 11, 11, VeryLowTier)
	object Swap11 extends OpCode(0x9a.toByte, 12, 12, VeryLowTier)
	object Swap12 extends OpCode(0x9b.toByte, 13, 13, VeryLowTier)
	object Swap13 extends OpCode(0x9c.toByte, 14, 14, VeryLowTier)
	object Swap14 extends OpCode(0x9d.toByte, 15, 15, VeryLowTier)
	object Swap15 extends OpCode(0x9e.toByte, 16, 16, VeryLowTier)
	object Swap16 extends OpCode(0x9f.toByte, 17, 17, VeryLowTier)

	//(0xa[n]) あるアドレスに対して、あるデータを、n個のタグとともにログ出力する。
	object Log0 extends OpCode(0xa0.toByte, 2, 0, SpecialTier)
	object Log1 extends OpCode(0xa1.toByte, 3, 0, SpecialTier)
	object Log2 extends OpCode(0xa2.toByte, 4, 0, SpecialTier)
	object Log3 extends OpCode(0xa3.toByte, 5, 0, SpecialTier)
	object Log4 extends OpCode(0xa4.toByte, 6, 0, SpecialTier)

	//システム操作。

	/**
	 * 0xf0：関連付けられたコードとともに、新しいアカウントを生成する。
	 */
	object Create extends OpCode(0xf0.toByte, 3, 1, SpecialTier)//[in_size] [in_offs] [gas_val] CREATE

	/**
	 * cxf1：あるアカウントに対してメッセージコールを実行する。
	 */
	object Call extends OpCode(0xf1.toByte, 7, 1, SpecialTier)//[out_data_size] [out_data_start] [in_data_size] [in_data_start] [value] [to_addr] [gas] CALL

	/**
	 * 0xf2：自分自身を実行するが、コードは自分のアドレスからではなくTOから取得する。
	 */
	object CallCode extends OpCode(0xf2.toByte, 7, 1, SpecialTier)

	/**
	 * 0xf3：実行を停止し、出力データを返す。
	 */
	object Return extends OpCode(0xf3.toByte, 2, 0, ZeroTier)

	/**
	 * 0xff：実行を停止し、アカウントを削除対象として登録する。
	 */
	object Suicide extends OpCode(0xff.toByte, 1, 0, ZeroTier)


	val all: Set[OpCode] = {
		LipicaMiscUtils.sealedDescendants[OpCode].getOrElse(Set.empty).asInstanceOf[Set[OpCode]]
	}

	/**
	 * バイトコード表す１バイトから、オペコードオブジェクトを引くための連想配列。
	 */
	private val byteToOpCodeObjMap: Map[Byte, OpCode] = all.map(each => each.opcode -> each).toMap

	/**
	 * バイトコード表す１バイトから、オペコードオブジェクトを引いて返します。
	 */
	def code(code: Byte): Option[OpCode] = this.byteToOpCodeObjMap.get(code)

}
