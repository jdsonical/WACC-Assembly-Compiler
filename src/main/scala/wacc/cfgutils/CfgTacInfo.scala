package wacc.cfgutils

import wacc.TAC
import wacc.TAC._
import wacc.cfgutils.CFG.Id

import scala.language.implicitConversions


object CfgTacInfo extends LiveRange {
  /** Given a TAC, return:
   * (the set of tRegisters it uses, the set of tRegisters it defines, the indices of its successor TACs). */
  override def getInfo(instr: TAC.TAC, program: Vector[TAC.TAC], id: Id): (Set[TAC.TRegister], Set[TAC.TRegister], Set[Id]) = {

    def getId(l: Label): Id = program.indexOf(l)

    implicit def toUsedRegisters(ops: List[Operand]): Set[TRegister] = ops.collect {
      case TRegister(num) => TRegister(num)
    }.toSet


    var uses: Set[TRegister] = Set()
    var defs: Set[TRegister] = Set()
    var succs: List[Id] = List(id + 1)

    instr match {
      case BinaryOpTAC(_, t1, t2, res) => {
        uses = List(t1, t2)
        defs = List(res)
      }
      case UnaryOpTAC(_, t1, res) => {
        uses = List(t1)
        defs = List(res)
      }
      case AssignmentTAC(t1, res) => {
        uses = List(t1)
        defs = List(res)
      }
      case IfTAC(t1, lbl) => {
        uses = List(t1)
        succs = List(id + 1, getId(lbl))
      }
      case EndFuncTAC() => {
        succs = Nil
      }
      case CommandTAC(_, t1, _) =>
        uses = List(t1)
      case PushParamTAC(t1) =>
        uses = List(t1)
      case PopParamTAC(_, t1, _) =>
        uses = List(t1)
      case CallTAC(lbl, args, dstReg) =>
        uses = args
        defs = List(dstReg)
        succs = List(id + 1, getId(lbl))
      case GOTO(lbl) =>
        succs = List(getId(lbl))
      case CreatePairElem(_, _, ptr, value) => //TODO: These pair-related ones could be wrong:
        uses = List(value)
        defs = List(ptr)
      case CreatePair(_, _, fstReg, sndReg, srcReg, ptrReg, dstReg) => //TODO
        print("CreatePair not yet translated in CFG.scala")
      case AliasedPushTAC(alias, _) => uses = List(alias)
      case AliasedPopTAC(_, alias) => defs = List(alias)

      case _ => println("WARNING: Unimplemented TAC in CfgTacInfo: " + instr + "\n\t-Treated as though it does nothing.")
    }
    (uses, defs, succs.toSet)
  }

  /** Replace all instances of a tRegister in the instruciton with a different tRegister */
  def mapTAC(tac: TAC, modification: (TRegister, TRegister)): TAC = {
    tac.getClass.getDeclaredFields.toList.foreach {
      field =>
        field.setAccessible(true)
        val currentVal = field.get(tac)
        val newVal: AnyRef = currentVal match {
          case reg: TRegister if (reg == modification._1) => modification._2
          case _ => currentVal
        }
        field.set(tac, newVal)
    }
    tac.asInstanceOf[tac.type]
  }
}


/*
Load t as proxy

 */