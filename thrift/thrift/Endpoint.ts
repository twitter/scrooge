/**
 * Generated by Scrooge
 *   version: 4.13.0-SNAPSHOT
 *   rev: b5540a5ea44ab683c536210956fb2e7c3497b122
 *   built at: 20170330-131250
 */
/**
 * Indicates the network context of a service recording an annotation with two
 * exceptions.
 *
 * When a BinaryAnnotation, and key is CLIENT_ADDR or SERVER_ADDR,
 * the endpoint indicates the source or destination of an RPC. This exception
 * allows zipkin to display network context of uninstrumented services, or
 * clients such as web browsers.
 */


import thrift from 'thrift'
import {Thrift, Protocol} from 'thrift'



export interface IEndpointArgs {
    ipv4: number
    port: number
    serviceName: string
}

export class Endpoint {
    public ipv4: number
    public port: number
    public serviceName: string
    constructor(args: IEndpointArgs) {
            if (args.ipv4 != null) {
                this.ipv4 = args.ipv4
            } else {
                throw new Thrift.TProtocolException(Thrift.TProtocolExceptionType.UNKNOWN, 'Required field ipv4 is unset!')
            }
            if (args.port != null) {
                this.port = args.port
            } else {
                throw new Thrift.TProtocolException(Thrift.TProtocolExceptionType.UNKNOWN, 'Required field port is unset!')
            }
            if (args.serviceName != null) {
                this.serviceName = args.serviceName
            } else {
                throw new Thrift.TProtocolException(Thrift.TProtocolExceptionType.UNKNOWN, 'Required field serviceName is unset!')
            }
    }

    public read(input: Protocol): void {
        input.readStructBegin()
        while (true) {
            const {fname, ftype, fid} = input.readFieldBegin()
            if (ftype === Thrift.Type.STOP) {
                break
            }
            switch (fid) {
                case 1:
                    this.ipv4 = input.readI32()
                    break
                case 2:
                    this.port = input.readI16()
                    break
                case 3:
                    this.serviceName = input.readString()
                    break
                default:
                    input.skip(ftype)
            }
            input.readFieldEnd()
        }
        input.readStructEnd()
        return
    }

    public write(output: Protocol): void {
        output.writeStructBegin("Endpoint")
        if (this.ipv4 != null) {
            if (true) {
                const ipv4_item = this.ipv4
                output.writeFieldBegin("ipv4", Thrift.Type.I32, 1)
                output.writeI32(ipv4_item)
                output.writeFieldEnd()
            }
        }
        if (this.port != null) {
            if (true) {
                const port_item = this.port
                output.writeFieldBegin("port", Thrift.Type.I16, 2)
                output.writeI16(port_item)
                output.writeFieldEnd()
            }
        }
        if (this.serviceName != null) {
            if (this.serviceName !== null) {
                const serviceName_item = this.serviceName
                output.writeFieldBegin("serviceName", Thrift.Type.STRING, 3)
                output.writeString(serviceName_item)
                output.writeFieldEnd()
            }
        }
        output.writeFieldStop()
        output.writeStructEnd()
        return
    }
}