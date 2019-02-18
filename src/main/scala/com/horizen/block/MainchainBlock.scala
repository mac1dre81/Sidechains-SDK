package com.horizen.block

import com.horizen.transaction.MC2SCAggregatedTransaction
import com.horizen.utils.{ByteArrayWrapper, BytesUtils}
import scorex.core.serialization.{BytesSerializable, Serializer}

import scala.util.{Failure, Success, Try}
import scala.collection.mutable.{Map}

// Mainchain Block structure:
//
// Field                Description                                             Size
// Magic no             value always 0xD9B4BEF9                                 4 bytes
// Blocksize            number of bytes following up to end of block            4 bytes
// Blockheader          consists of 7 items (see @MainchainHeader)              80+32 bytes
// Sidechain counter    positive integer (number of SC mentioned in block)      1-9 bytes
// SCMap                map with merkle roots for each Sidechain transactions   <Sidechain counter> * (32 + 32)
// Transaction counter  positive integer (number of transactions in block)      1-9 bytes
// Transactions         the (non empty) list of transactions                    depends on <Transaction counter>

class MainchainBlock(
                    val header: MainchainHeader,
                    val sidechainRelatedAggregatedTransaction: Option[MC2SCAggregatedTransaction]
                    ) extends BytesSerializable {

  def semanticValidity(): Boolean = ???

  override type M = MainchainBlock

  override def serializer: Serializer[MainchainBlock] = MainchainBlockSerializer
}


object MainchainBlock {
  // TO DO: check size
  val MAX_MAINCHAIN_BLOCK_SIZE = 2048 * 1024 //2048K

  def create(mainchainBlockBytes: Array[Byte], sidechainId: Array[Byte]): Try[MainchainBlock] = {
    require(mainchainBlockBytes.length < MAX_MAINCHAIN_BLOCK_SIZE)
    require(sidechainId.length == 32)

    parseMainchainBlockBytes(mainchainBlockBytes) match {
      case Success((header, scmap, transactions)) =>
        val mc2scTransaction: Option[MC2SCAggregatedTransaction] = calculateMC2SCTransaction(sidechainId, transactions)
        val block = new MainchainBlock(header, mc2scTransaction)
        if(!block.semanticValidity())
          Failure(new Exception("Mainchain Block bytes were parsed, but lead to not semantically valid data."))
        else
          Success(block)
      case Failure(e) =>
        Failure(e)
    }
  }

  // Try to parse Mainchain block and return MainchainHeader, SCMap and Transactions bytes sequence.
  private def parseMainchainBlockBytes(mainchainBlockBytes: Array[Byte]): Try[(MainchainHeader, Map[ByteArrayWrapper, Array[Byte]], Seq[Array[Byte]])] = Try {
    var offset: Int = 0

    val magicNumber: Array[Byte] = mainchainBlockBytes.slice(offset, offset + 4)
    offset += 4
    if(!BytesUtils.toHexString(magicNumber).equals("d9b4bef9"))
      throw new IllegalArgumentException("Input data corrupted. Magic number is different")

    val blockSize: Int = BytesUtils.getInt(mainchainBlockBytes, offset)
    offset += 4
    if(blockSize != magicNumber.length - offset)
      throw new IllegalArgumentException("Input data corrupted. Actual block size different to decalred one.")

    MainchainHeader.create(mainchainBlockBytes.slice(offset, offset + MainchainHeader.HEADER_SIZE)) match {
      case Success(header) =>
        offset += MainchainHeader.HEADER_SIZE
        var SCMapItemsSize: Long = 0
        // try parse var length int
        mainchainBlockBytes(offset) match {
          case 253 =>
            SCMapItemsSize = BytesUtils.getShort(mainchainBlockBytes, offset)
            offset += 2
          case 254 =>
            SCMapItemsSize = BytesUtils.getInt(mainchainBlockBytes, offset)
            offset += 4
          case 255 =>
            SCMapItemsSize = BytesUtils.getLong(mainchainBlockBytes, offset)
            offset += 8
          case _ =>
            SCMapItemsSize = mainchainBlockBytes(offset).longValue()
            offset += 1
        }

        // parse SCMap
        val SCMap: Map[ByteArrayWrapper, Array[Byte]] = Map[ByteArrayWrapper, Array[Byte]]()
        val SCMapStartingOffset = offset
        while(offset != SCMapStartingOffset + SCMapItemsSize * 64) {
          SCMap.put(
            new ByteArrayWrapper(mainchainBlockBytes.slice(offset, offset + 32)),
            mainchainBlockBytes.slice(offset + 32, offset + 64)
          )
          offset += 64
        }

        var transactionsSize: Long = 0
        // try parse var length int
        mainchainBlockBytes(offset) match {
          case 253 =>
            transactionsSize = BytesUtils.getShort(mainchainBlockBytes, offset)
            offset += 2
          case 254 =>
            transactionsSize = BytesUtils.getInt(mainchainBlockBytes, offset)
            offset += 4
          case 255 =>
            transactionsSize = BytesUtils.getLong(mainchainBlockBytes, offset)
            offset += 8
          case _ =>
            transactionsSize = mainchainBlockBytes(offset).longValue()
            offset += 1
        }

        // parse SCMap
        val transactions: Seq[Array[Byte]] = Seq[Array[Byte]]()

        while(offset != mainchainBlockBytes.length) {
          //  TO DO: parse transactions one by one
        }

        (header, SCMap, transactions)
      case Failure(e) =>
        throw e
    }

  }

  private def calculateMC2SCTransaction(sidechainID: Array[Byte], transactions: Seq[Array[Byte]]): Option[MC2SCAggregatedTransaction] = ???
}



object MainchainBlockSerializer extends Serializer[MainchainBlock] {
  override def toBytes(obj: MainchainBlock): Array[Byte] = ???

  override def parseBytes(bytes: Array[Byte]): Try[MainchainBlock] = ???
}