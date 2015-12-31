package org.lipicalabs.lipica.core.vm

import org.lipicalabs.lipica.core.vm.Tier._

import scala.collection.mutable


/**
 * Lipica VM の命令が属するティア。
 * このlevel値が、１コードあたり
 * 消費されるマナの量に等しい（もしくは比例する）。
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
	private val set = new mutable.HashSet[Tier]

	object ZeroTier extends Tier {
		override val level = 0
	}
	set.add(ZeroTier)
	object BaseTier extends Tier {
		override val level = 2
	}
	set.add(BaseTier)
	object VeryLowTier extends Tier {
		override val level = 3
	}
	set.add(VeryLowTier)
	object LowTier extends Tier {
		override val level = 5
	}
	set.add(LowTier)
	object MidTier extends Tier {
		override val level = 8
	}
	set.add(MidTier)
	object HighTier extends Tier {
		override val level = 10
	}
	set.add(HighTier)
	object ExtTier extends Tier {
		override val level = 20
	}
	set.add(ExtTier)
	object SpecialTier extends Tier {
		override val level = 1
	}
	set.add(SpecialTier)
	object InvalidTier extends Tier {
		override val level = 0
	}
	set.add(InvalidTier)

	val all: Set[Tier] = {
		this.set.toSet
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
	override def toString: String = this.name
}


object OpCode {

	private val set = new mutable.HashSet[OpCode]

	/** 0x00：停止。*/
	object Stop extends OpCode(0x00, 0, 0, ZeroTier)
	set.add(Stop)

	//以下、算術演算。
	/**
	 * 0x01：加算。
	 */
	object Add extends OpCode(0x01, 2, 1, VeryLowTier)
	set.add(Add)

	/**
	 * 0x02：乗算。
	 */
	object Mul extends OpCode(0x02, 2, 1, LowTier)
	set.add(Mul)

	/**
	 * 0x03：減算。
	 */
	object Sub extends OpCode(0x03, 2, 1, VeryLowTier)
	set.add(Sub)

	/**
	 * 0x04：整数除算。
	 */
	object Div extends OpCode(0x04, 2, 1, LowTier)
	set.add(Div)

	/**
	 * 0x05：符号付き整数除算。
	 */
	object SDiv extends OpCode(0x05, 2, 1, LowTier)
	set.add(SDiv)

	/**
	 * 0x06：剰余演算。
	 */
	object Mod extends OpCode(0x06, 2, 1, LowTier)
	set.add(Mod)

	/**
	 * 0x07：符号付き剰余演算。
	 */
	object SMod extends OpCode(0x07, 2, 1, LowTier)
	set.add(SMod)

	/**
	 * 0x08：加算と剰余演算との組み合わせ。
	 */
	object AddMod extends OpCode(0x08, 3, 1, MidTier)
	set.add(AddMod)

	/**
	 * 0x09：乗算と剰余演算との組み合わせ。
	 */
	object MulMod extends OpCode(0x09, 3, 1, MidTier)
	set.add(MulMod)

	/**
	 * 0x0a：べき乗。
	 */
	object Exp extends OpCode(0x0a, 2, 1, SpecialTier)
	set.add(Exp)

	/**
	 * 0x0b：符号付き整数において、符号部分のバイト数を下位ビットに拡張する。
	 */
	object SignExtend extends OpCode(0x0b, 2, 1, LowTier)
	set.add(SignExtend)

	//以下、比較／論理演算。
	/**
	 * 0x10：未満。
	 */
	object LT extends OpCode(0x10, 2, 1, VeryLowTier)
	set.add(LT)

	/**
	 * 0x11：超。
	 */
	object GT extends OpCode(0x11, 2, 1, VeryLowTier)
	set.add(GT)

	/**
	 * 0x12：符号付きLT。
	 */
	object SLT extends OpCode(0x12, 2, 1, VeryLowTier)
	set.add(SLT)

	/**
	 * 0x13：符号付きGT。
	 */
	object SGT extends OpCode(0x13, 2, 1, VeryLowTier)
	set.add(SGT)

	/**
	 * 0x14：等値。
	 */
	object Eq extends OpCode(0x14, 2, 1, VeryLowTier)
	set.add(Eq)

	/**
	 * 0x15：命題論理における否定。
	 */
	object IsZero extends OpCode(0x15, 1, 1, VeryLowTier)
	set.add(IsZero)

	/**
	 * 0x16：ビットごとの論理積。
	 */
	object And extends OpCode(0x16, 2, 1, VeryLowTier)
	set.add(And)

	/**
	 * 0x17：ビットごとの論理和。
	 */
	object Or extends OpCode(0x17, 2, 1, VeryLowTier)
	set.add(Or)

	/**
	 * 0x18：ビットごとのXOR。
	 */
	object Xor extends OpCode(0x18, 2, 1, VeryLowTier)
	set.add(Xor)

	/**
	 * 0x19：ビットごとの反転。
	 */
	object Not extends OpCode(0x19, 1, 1, VeryLowTier)
	set.add(Not)

	/**
	 * 0x1a：Wordから添字によって１バイトを読み取る。
	 */
	object Byte extends OpCode(0x1a, 2, 1, VeryLowTier)
	set.add(Byte)

	//暗号学的操作。

	/**
	 * 0x20：Keccak-256ダイジェスト値を計算する。
	 */
	object Keccak256 extends OpCode(0x20, 2, 1, SpecialTier)
	set.add(Keccak256)

	//環境情報。

	/**
	 * 0x30：現在実行中のアカウントのアドレスを取得する。
	 */
	object Address extends OpCode(0x30, 0, 1, BaseTier)
	set.add(Address)

	/**
	 * 0x31：渡されたアカウントの現在の残高を取得する。
	 */
	object Balance extends OpCode(0x31, 1, 1, ExtTier)
	set.add(Balance)

	/**
	 * 0x32：実行を開始したアドレスを取得する。（コントラクト用アドレスであることはない。）
	 */
	object Origin extends OpCode(0x32, 0, 1, BaseTier)
	set.add(Origin)

	/**
	 * 0x33：直接の呼び出し元アドレスを取得する。
	 */
	object Caller extends OpCode(0x33, 0, 1, BaseTier)
	set.add(Caller)

	/**
	 * 0x34：この実行に責任がある処理によって入金された額を取得する。
	 */
	object CallValue extends OpCode(0x34, 0, 1, BaseTier)
	set.add(CallValue)

	/**
	 * 0x35：現在の環境における入力データを取得する。
	 */
	object CallDataLoad extends OpCode(0x35, 1, 1, VeryLowTier)
	set.add(CallDataLoad)

	/**
	 * 0x36：現在の環境における入力データの長さを取得する。
	 */
	object CallDataSize extends OpCode(0x36, 0, 1, VeryLowTier)
	set.add(CallDataSize)

	/**
	 * 0x37：現在の環境における入力データをメモリにコピーする。
	 */
	object CallDataCopy extends OpCode(0x37, 3, 0, VeryLowTier)
	set.add(CallDataCopy)

	/**
	 * 0x38：現在の環境で動作しているコードの長さを取得する。
	 */
	object CodeSize extends OpCode(0x38, 0, 1, BaseTier)
	set.add(CodeSize)

	/**
	 * 0x39：現在の環境で動作しているコードをメモリにコピーする。
	 */
	object CodeCopy extends OpCode(0x39, 3, 0, VeryLowTier)
	set.add(CodeCopy)

	/**
	 * 0x3a：１マナの価格を返します。
	 */
	object ManaPrice extends OpCode(0x3a, 0, 1, VeryLowTier)
	set.add(ManaPrice)

	/**
	 * 0x3b：現在の環境で動作しているコードのオフセット以後の長さを取得する。
	 */
	object ExtCodeSize extends OpCode(0x3b, 1, 1, ExtTier)
	set.add(ExtCodeSize)

	/**
	 * 0x3c：現在の環境で動作しているコードのオフセット以後の部分をメモリにコピーする。
	 */
	object ExtCodeCopy extends OpCode(0x3c, 4, 0, ExtTier)
	set.add(ExtCodeCopy)

	//ブロック情報。

	/**
	 * 0x40：最近の完全なブロックのハッシュ値を取得する。
	 */
	object BlockHash extends OpCode(0x40, 1, 1, ExtTier)
	set.add(BlockHash)

	/**
	 * 0x41：ブロックのコインベースアドレスを取得する。
	 */
	object Coinbase extends OpCode(0x41, 0, 1, BaseTier)
	set.add(Coinbase)

	/**
	 * 0x42：ブロックのタイムスタンプを取得する。
	 */
	object Timestamp extends OpCode(0x42, 0, 1, BaseTier)
	set.add(Timestamp)

	/**
	 * 0x43：ブロックの番号を取得する。
	 */
	object BlockNumber extends OpCode(0x43, 0, 1, BaseTier)
	set.add(BlockNumber)

	/**
	 * 0x44：ブロックの難度を取得する。
	 */
	object Difficulty extends OpCode(0x44, 0, 1, BaseTier)
	set.add(Difficulty)

	/**
	 * 0x45：ブロックのマナ上限を取得する。
	 */
	object ManaLimit extends OpCode(0x45, 0, 1, BaseTier)
	set.add(ManaLimit)

	//メモリ、ストレージ、制御に関する操作。

	/**
	 * 0x50：スタックから要素を取り出す。
	 */
	object Pop extends OpCode(0x50, 1, 0, BaseTier)
	set.add(Pop)

	/**
	 * 0x51：メモリから１ワードを読み取る。
	 */
	object MLoad extends OpCode(0x51, 1, 1, VeryLowTier)
	set.add(MLoad)

	/**
	 * 0x52：メモリに１ワードを書き込む。
	 */
	object MStore extends OpCode(0x52, 2, 0, VeryLowTier)
	set.add(MStore)

	/**
	 * 0x53：メモリに１バイトを書き込む。
	 */
	object MStore8 extends OpCode(0x53, 2, 0, VeryLowTier)
	set.add(MStore8)

	/**
	 * 0x54：ストレージから１ワードを読み取る。
	 */
	object SLoad extends OpCode(0x54, 1, 1, SpecialTier)
	set.add(SLoad)

	/**
	 * 0x55：ストレージに１ワードを記録する。
	 */
	object SStore extends OpCode(0x55, 2, 0, SpecialTier)
	set.add(SStore)

	/**
	 * 0x56：プログラムカウンタを変更する。
	 */
	object Jump extends OpCode(0x56, 1, 0, MidTier)
	set.add(Jump)

	/**
	 * 0x57：条件に応じて、プログラムカウンタを変更する。
	 */
	object JumpI extends OpCode(0x57, 2, 0, HighTier)
	set.add(JumpI)

	/**
	 * 0x58：プログラムカウンタを取得する。
	 */
	object PC extends OpCode(0x58, 0, 1, BaseTier)
	set.add(PC)

	/**
	 * 0x59：アクティブメモリの容量を取得する。
	 */
	object MSize extends OpCode(0x59, 0, 1, BaseTier)
	set.add(MSize)

	/**
	 * 0x58：利用できるマナの量を取得する。
	 */
	object Mana extends OpCode(0x5a, 0, 1, BaseTier)
	set.add(Mana)

	/**
	 * 0x5b：
	 */
	object JumpDest extends OpCode(0x5b, 0, 0, SpecialTier)
	set.add(JumpDest)

	//プッシュ操作。
	//現在の文脈のプログラムコードの先を、
	//1バイト～32バイトのデータをスタックにプッシュする。
	object Push1 extends OpCode(0x60, 0, 1, VeryLowTier)
	set.add(Push1)
	object Push2 extends OpCode(0x61, 0, 1, VeryLowTier)
	set.add(Push2)
	object Push3 extends OpCode(0x62, 0, 1, VeryLowTier)
	set.add(Push3)
	object Push4 extends OpCode(0x63, 0, 1, VeryLowTier)
	set.add(Push4)
	object Push5 extends OpCode(0x64, 0, 1, VeryLowTier)
	set.add(Push5)
	object Push6 extends OpCode(0x65, 0, 1, VeryLowTier)
	set.add(Push6)
	object Push7 extends OpCode(0x66, 0, 1, VeryLowTier)
	set.add(Push7)
	object Push8 extends OpCode(0x67, 0, 1, VeryLowTier)
	set.add(Push8)
	object Push9 extends OpCode(0x68, 0, 1, VeryLowTier)
	set.add(Push9)
	object Push10 extends OpCode(0x69, 0, 1, VeryLowTier)
	set.add(Push10)
	object Push11 extends OpCode(0x6a, 0, 1, VeryLowTier)
	set.add(Push11)
	object Push12 extends OpCode(0x6b, 0, 1, VeryLowTier)
	set.add(Push12)
	object Push13 extends OpCode(0x6c, 0, 1, VeryLowTier)
	set.add(Push13)
	object Push14 extends OpCode(0x6d, 0, 1, VeryLowTier)
	set.add(Push14)
	object Push15 extends OpCode(0x6e, 0, 1, VeryLowTier)
	set.add(Push15)
	object Push16 extends OpCode(0x6f, 0, 1, VeryLowTier)
	set.add(Push16)
	object Push17 extends OpCode(0x70, 0, 1, VeryLowTier)
	set.add(Push17)
	object Push18 extends OpCode(0x71, 0, 1, VeryLowTier)
	set.add(Push18)
	object Push19 extends OpCode(0x72, 0, 1, VeryLowTier)
	set.add(Push19)
	object Push20 extends OpCode(0x73, 0, 1, VeryLowTier)
	set.add(Push20)
	object Push21 extends OpCode(0x74, 0, 1, VeryLowTier)
	set.add(Push21)
	object Push22 extends OpCode(0x75, 0, 1, VeryLowTier)
	set.add(Push22)
	object Push23 extends OpCode(0x76, 0, 1, VeryLowTier)
	set.add(Push23)
	object Push24 extends OpCode(0x77, 0, 1, VeryLowTier)
	set.add(Push24)
	object Push25 extends OpCode(0x78, 0, 1, VeryLowTier)
	set.add(Push25)
	object Push26 extends OpCode(0x79, 0, 1, VeryLowTier)
	set.add(Push26)
	object Push27 extends OpCode(0x7a, 0, 1, VeryLowTier)
	set.add(Push27)
	object Push28 extends OpCode(0x7b, 0, 1, VeryLowTier)
	set.add(Push28)
	object Push29 extends OpCode(0x7c, 0, 1, VeryLowTier)
	set.add(Push29)
	object Push30 extends OpCode(0x7d, 0, 1, VeryLowTier)
	set.add(Push30)
	object Push31 extends OpCode(0x7e, 0, 1, VeryLowTier)
	set.add(Push31)
	object Push32 extends OpCode(0x7f, 0, 1, VeryLowTier)
	set.add(Push32)

	//スタックの1番目～16番目のデータを複製する。
	object Dup1 extends OpCode(0x80.toByte, 1, 2, VeryLowTier)
	set.add(Dup1)
	object Dup2 extends OpCode(0x81.toByte, 2, 3, VeryLowTier)
	set.add(Dup2)
	object Dup3 extends OpCode(0x82.toByte, 3, 4, VeryLowTier)
	set.add(Dup3)
	object Dup4 extends OpCode(0x83.toByte, 4, 5, VeryLowTier)
	set.add(Dup4)
	object Dup5 extends OpCode(0x84.toByte, 5, 6, VeryLowTier)
	set.add(Dup5)
	object Dup6 extends OpCode(0x85.toByte, 6, 7, VeryLowTier)
	set.add(Dup6)
	object Dup7 extends OpCode(0x86.toByte, 7, 8, VeryLowTier)
	set.add(Dup7)
	object Dup8 extends OpCode(0x87.toByte, 8, 9, VeryLowTier)
	set.add(Dup8)
	object Dup9 extends OpCode(0x88.toByte, 9, 10, VeryLowTier)
	set.add(Dup9)
	object Dup10 extends OpCode(0x89.toByte, 10, 11, VeryLowTier)
	set.add(Dup10)
	object Dup11 extends OpCode(0x8a.toByte, 11, 12, VeryLowTier)
	set.add(Dup11)
	object Dup12 extends OpCode(0x8b.toByte, 12, 13, VeryLowTier)
	set.add(Dup12)
	object Dup13 extends OpCode(0x8c.toByte, 13, 14, VeryLowTier)
	set.add(Dup13)
	object Dup14 extends OpCode(0x8d.toByte, 14, 15, VeryLowTier)
	set.add(Dup14)
	object Dup15 extends OpCode(0x8e.toByte, 15, 16, VeryLowTier)
	set.add(Dup15)
	object Dup16 extends OpCode(0x8f.toByte, 16, 17, VeryLowTier)
	set.add(Dup16)

	//スタックの2番目～17番目のデータを、1番目のデータと交換する。
	object Swap1 extends OpCode(0x90.toByte, 2, 2, VeryLowTier)
	set.add(Swap1)
	object Swap2 extends OpCode(0x91.toByte, 3, 3, VeryLowTier)
	set.add(Swap2)
	object Swap3 extends OpCode(0x92.toByte, 4, 4, VeryLowTier)
	set.add(Swap3)
	object Swap4 extends OpCode(0x93.toByte, 5, 5, VeryLowTier)
	set.add(Swap4)
	object Swap5 extends OpCode(0x94.toByte, 6, 6, VeryLowTier)
	set.add(Swap5)
	object Swap6 extends OpCode(0x95.toByte, 7, 7, VeryLowTier)
	set.add(Swap6)
	object Swap7 extends OpCode(0x96.toByte, 8, 8, VeryLowTier)
	set.add(Swap7)
	object Swap8 extends OpCode(0x97.toByte, 9, 9, VeryLowTier)
	set.add(Swap8)
	object Swap9 extends OpCode(0x98.toByte, 10, 10, VeryLowTier)
	set.add(Swap9)
	object Swap10 extends OpCode(0x99.toByte, 11, 11, VeryLowTier)
	set.add(Swap10)
	object Swap11 extends OpCode(0x9a.toByte, 12, 12, VeryLowTier)
	set.add(Swap11)
	object Swap12 extends OpCode(0x9b.toByte, 13, 13, VeryLowTier)
	set.add(Swap12)
	object Swap13 extends OpCode(0x9c.toByte, 14, 14, VeryLowTier)
	set.add(Swap13)
	object Swap14 extends OpCode(0x9d.toByte, 15, 15, VeryLowTier)
	set.add(Swap14)
	object Swap15 extends OpCode(0x9e.toByte, 16, 16, VeryLowTier)
	set.add(Swap15)
	object Swap16 extends OpCode(0x9f.toByte, 17, 17, VeryLowTier)
	set.add(Swap16)

	//(0xa[n]) あるアドレスに対して、あるデータを、n個のタグとともにログ出力する。
	object Log0 extends OpCode(0xa0.toByte, 2, 0, SpecialTier)
	set.add(Log0)
	object Log1 extends OpCode(0xa1.toByte, 3, 0, SpecialTier)
	set.add(Log1)
	object Log2 extends OpCode(0xa2.toByte, 4, 0, SpecialTier)
	set.add(Log2)
	object Log3 extends OpCode(0xa3.toByte, 5, 0, SpecialTier)
	set.add(Log3)
	object Log4 extends OpCode(0xa4.toByte, 6, 0, SpecialTier)
	set.add(Log4)

	//システム操作。

	/**
	 * 0xf0：関連付けられたコードとともに、新しいアカウントを生成する。
	 */
	object Create extends OpCode(0xf0.toByte, 3, 1, SpecialTier)//[in_size] [in_offs] [gas_val] CREATE
	set.add(Create)

	/**
	 * cxf1：あるアカウントに対してメッセージコールを実行する。
	 */
	object Call extends OpCode(0xf1.toByte, 7, 1, SpecialTier)//[out_data_size] [out_data_start] [in_data_size] [in_data_start] [value] [to_addr] [gas] CALL
	set.add(Call)

	/**
	 * 0xf2：自分自身を実行するが、コードは自分のアドレスからではなくTOから取得する。
	 */
	object CallCode extends OpCode(0xf2.toByte, 7, 1, SpecialTier)
	set.add(CallCode)

	/**
	 * 0xf3：実行を停止し、出力データを返す。
	 */
	object Return extends OpCode(0xf3.toByte, 2, 0, ZeroTier)
	set.add(Return)

	/**
	 * 0xff：実行を停止し、アカウントを削除対象として登録する。
	 */
	object Suicide extends OpCode(0xff.toByte, 1, 0, ZeroTier)
	set.add(Suicide)

	val all: Set[OpCode] = this.set.toSet

	/**
	 * バイトコード表す１バイトから、オペコードオブジェクトを引くための連想配列。
	 */
	private val byteToOpCodeObjMap: Map[Byte, OpCode] = all.map(each => each.opcode -> each).toMap

	/**
	 * バイトコード表す１バイトから、オペコードオブジェクトを引いて返します。
	 */
	def code(code: Byte): Option[OpCode] = this.byteToOpCodeObjMap.get(code)

}
