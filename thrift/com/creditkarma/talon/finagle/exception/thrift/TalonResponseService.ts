/**
 * Generated by Scrooge
 *   version: 4.13.0-SNAPSHOT
 *   rev: b5540a5ea44ab683c536210956fb2e7c3497b122
 *   built at: 20170330-131233
 */
import thrift from 'thrift'
import {Thrift, Protocol} from 'thrift'

import { TalonResponse } from './TalonResponse'
import { TalonResponseException } from './TalonResponseException'



export class TalonResponseServiceInternalTalonResponseArgs {
    constructor(args?: {}) {
    }

    public read(input: Protocol): void {
        input.readStructBegin()
        while (true) {
            const {fname, ftype, fid} = input.readFieldBegin()
            if (ftype === Thrift.Type.STOP) {
                break
            }
            input.skip(ftype)
            input.readFieldEnd()
        }
        input.readStructEnd()
        return
    }

    public write(output: Protocol): void {
        output.writeStructBegin("TalonResponseServiceInternalTalonResponseArgs")
        output.writeFieldStop()
        output.writeStructEnd()
        return
    }
}

export class TalonResponseServiceInternalTalonResponseResult {
    public success: void
    public ex: TalonResponseException
    constructor(args?: {}) {
            if (args.ex != null) {
                this.ex = args.ex
            } else {
                throw new Thrift.TProtocolException(Thrift.TProtocolExceptionType.UNKNOWN, 'Required field ex is unset!')
            }
    }

    public read(input: Protocol): void {
        input.readStructBegin()
        while (true) {
            const {fname, ftype, fid} = input.readFieldBegin()
            if (ftype === Thrift.Type.STOP) {
                break
            }
            input.skip(ftype)
            input.readFieldEnd()
        }
        input.readStructEnd()
        return
    }

    public write(output: Protocol): void {
        output.writeStructBegin("TalonResponseServiceInternalTalonResponseResult")
        if (this.ex != null) {
            if (this.ex !== null) {
                const ex_item = this.ex
                output.writeFieldBegin("ex", Thrift.Type.STRUCT, 1)
                ex_item.write(output)
                output.writeFieldEnd()
            }
        }
        output.writeFieldStop()
        output.writeStructEnd()
        return
    }
}
export class Client {
    private _seqid: number
    public _reqs: {[key: string]: (e: Error, r: any) => void}

    constructor(public output: Protocol, public pClass: Protocol) {
        this._seqid = 0
        this._reqs = {}
    }

    public seqid(): number {
        return this._seqid
    }

    public new_seqid(): number {
        return this._seqid += 1
    }

    public internalTalonResponse(callback: (e: Error, r: void) => void): void
    public internalTalonResponse(): Promise<void>
    public internalTalonResponse(callback?: (e: Error, r: void) => void): Promise<void>|void {
        this._seqid = this.new_seqid(V)
        if (callback instanceof Function) {
            return new Promise((resolve, reject) => {
                this._reqs[this.seqid()] = function(error, result) {
                    if (error) {
                        reject(error)
                    } else {
                        resolve(result)
                    }
                }
                this.send_internalTalonResponse()
            })
        } else {
            this._reqs[this.seqid()] = callback
            this.send_internalTalonResponse()
        }
    }

    public send_internalTalonResponse(): void {
        const output = new this.pClass(this.output)
        output.writeMessageBegin("internalTalonResponse", Thrift.MessageType.CALL, this.seqid())
        const args = new TalonResponseServiceInternalTalonResponseArgs({  })
        args.write(output)
        output.writeMessageEnd()
        return this.output.flush()
    }

    public recv_internalTalonResponse(input: Protocol, mtype: Thrift.MessageType, rseqid: number): void {
        const noop = () => null
        let callback = this._reqs[rseqid] || noop
        delete this._reqs[rseqid]
        if (mtype === Thrift.MessageType.EXCEPTION) {
            const x = new Thrift.TApplicationException()
            x.read(input)
            input.readMessageEnd()
            return callback(x)
        }
        const result = new TalonResponseServiceInternalTalonResponseResult()
        result.read(input)
        input.readMessageEnd()

        if (result.ex != null) {
            return callback(result.ex)
        }
        if (result.success != null) {
            return callback(null, result.success)
        }
        return callback("internalTalonResponse failed: unknown result")
    }
    }

export class Processor {
    private _handler

    constructor(handler) {
        this._handler = handler
    }

    public process(input: Protocol, output: Protocol) {
        const r = input.readMessageBegin()
        if (this["process_" + r.fname]) {
            return this["process_" + r.fname].call(this, r.rseqid, input, output)
        } else {
            input.skip(Thrift.Type.STRUCT)
            input.readMessageEnd()
            const err = `Unknown function ${r.fname}`
            const x = new Thrift.TApplicationException(Thrift.TApplicationExceptionType.UNKNOWN_METHOD, err)
            output.writeMessageBegin(r.fname, Thrift.MessageType.EXCEPTION, r.rseqid)
            x.write(output)
            output.writeMessageEnd()
            output.flush()
        }
    }

    public process_internalTalonResponse(seqid: number, input: Protocol, output: Protocol) {
        const args = new TalonResponseServiceInternalTalonResponseArgs()
        args.read(input)
        input.readMessageEnd()
        if (this._handler.internalTalonResponse.length === 0) {
            Promise.resolve(this._handler.internalTalonResponse).then((data) => {
                const result = new TalonResponseServiceInternalTalonResponseResult({success: data})
                output.writeMessageBegin("internalTalonResponse", Thrift.MessageType.REPLY, seqid)
                result.write(output)
                output.writeMessageEnd()
                output.flush()
            }, (err) => {
                const result = new Thrift.TApplicationException(Thrift.TApplicationExceptionType.UNKNOWN, err.message)
                output.writeMessageBegin("internalTalonResponse", Thrift.MessageType.EXCEPTION, seqid)
                result.write(output)
                output.writeMessageEnd()
                output.flush()
            })
        } else {
            this._handler.internalTalonResponse((err, data) => {
                let result
                if (err == null) {
                    result = new TalonResponseServiceInternalTalonResponseResult((err != null ? err : {success: data}))
                    output.writeMessageBegin("internalTalonResponse", Thrift.MessageType.REPLY, seqid)
                } else {
                    result = new Thrift.TApplicationException(Thrift.TApplicationExceptionType.UNKNOWN, err.message)
                    output.writeMessageBegin("internalTalonResponse", Thrift.MessageType.EXCEPTION, seqid)
                }
                result.write(output)
                output.writeMessageEnd()
                output.flush()
            })
        }
    }
    
}
