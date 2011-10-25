package com.gigaspaces.localview;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceLeaseExpiration;
import com.gigaspaces.annotation.pojo.SpaceVersion;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "Stock")
@SpaceClass(persist = true)
public class Stock implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -3116349899453529328L;
	private Integer stockId;
    private String stockName;
    private long lease;
    private int version;
    private Integer data;

    public Stock() {

    }

    public Stock(Integer stockId) {
        this.stockId = stockId;
    }

    @Id
    @SpaceId
    @Column(name = "ID")
    public Integer getStockId() {
        return stockId;
    }

    public void setStockId(Integer stockId) {
        this.stockId = stockId;
    }

    @Column(name = "StockName")
    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    @Column(name = "Lease")
    @SpaceLeaseExpiration
    public long getLease() {
        return lease;
    }

    public void setLease(long lease) {
        this.lease = lease;
    }

    @Column(name = "Version")
    @SpaceVersion
    public int getVersion() {
        return version;
    }

    public Object setVersion(int version) {
        this.version = version;
        return null;
    }

    public Integer getData() {
        return data;
    }

    public void setData(Integer data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Stock stock = (Stock) o;

        if (version != stock.version) return false;
        if (stockId != null ? !stockId.equals(stock.stockId) : stock.stockId != null) return false;
        if (stockName != null ? !stockName.equals(stock.stockName) : stock.stockName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = stockId != null ? stockId.hashCode() : 0;
        result = 31 * result + (stockName != null ? stockName.hashCode() : 0);
        result = 31 * result + version;
        return result;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "stockId=" + stockId +
                ", stockName='" + stockName + '\'' +
                ", lease=" + lease +
                ", version=" + version +
                '}';
    }
}
