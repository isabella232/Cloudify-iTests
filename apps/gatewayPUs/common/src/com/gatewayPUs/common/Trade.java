package com.gatewayPUs.common;

import com.gigaspaces.annotation.pojo.*;
import com.gigaspaces.annotation.pojo.SpaceProperty.IndexType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.io.Serializable;
import java.util.Arrays;

@Entity
@Table(name = "Trade")
public class Trade implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer symbolLabel;
    private Float price;
    private Long timestamp;
    private byte[] payload;
    private Integer quantity;
    private Integer version = 0;
    private long lease;
    transient private String uid;

    public Trade() {
    }

    public Trade(Integer id) {
        symbolLabel = id;
    }

    @SpaceRouting
    @SpaceProperty(index = IndexType.BASIC)
    public Integer getSymbolLabel() {
        return symbolLabel;
    }

    public void setSymbolLabel(Integer symbolLabel) {
        this.symbolLabel = symbolLabel;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    @SpaceId(autoGenerate = true)
    @Id
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @SpaceVersion
    public int getVersion() {
        return version;

    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Column(name = "Lease")
    @SpaceLeaseExpiration
    public long getLease() {
        return lease;
    }

    public void setLease(long lease) {
        this.lease = lease;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Trade trade = (Trade) o;

        if (!Arrays.equals(payload, trade.payload)) return false;
        if (price != null ? !price.equals(trade.price) : trade.price != null) return false;
        if (quantity != null ? !quantity.equals(trade.quantity) : trade.quantity != null) return false;
        if (symbolLabel != null ? !symbolLabel.equals(trade.symbolLabel) : trade.symbolLabel != null) return false;
        if (timestamp != null ? !timestamp.equals(trade.timestamp) : trade.timestamp != null) return false;
        if (version != null ? !version.equals(trade.version) : trade.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = symbolLabel != null ? symbolLabel.hashCode() : 0;
        result = 31 * result + (price != null ? price.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (payload != null ? Arrays.hashCode(payload) : 0);
        result = 31 * result + (quantity != null ? quantity.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "symbolLabel=" + symbolLabel +
                ", price=" + price +
                ", timestamp=" + timestamp +
                ", payload=" + payload +
                ", quantity=" + quantity +
                ", version=" + version +
                ", lease=" + lease +
                ", uid='" + uid + '\'' +
                '}';
    }
}
