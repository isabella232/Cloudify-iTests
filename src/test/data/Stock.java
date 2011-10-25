package test.data;

import com.gigaspaces.annotation.pojo.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name="Stock")
@SpaceClass(persist = true)
public class Stock implements Serializable {
    private Integer stockId;
    private String stockName;
    private Integer rate;
    private long lease;
    private int version;

    public Stock()
    {

    }

    public Stock(Integer stockId)
    {
        this.stockId=stockId;
    }

    @Id
    @SpaceId
    public Integer getStockId()
    {
        return stockId;
    }
    
    public void setStockId(Integer stockId)
    {
        this.stockId = stockId;
    }

    @Column(name="StockName")
    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    @Column(name="Lease")
    @SpaceLeaseExpiration
    public long getLease() {
        return lease;
    }

    public void setLease(long lease) {
        this.lease = lease;
    }

    public Integer getRate() {
        return rate;
    }

    public void setRate(Integer rate) {
        this.rate = rate;
    }

    @Column(name="Version")
    @SpaceVersion
    public int getVersion() {
        return version;
    }

    public Object setVersion(int version) {
        this.version = version;
        return null;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Stock stock = (Stock) o;

        if (stockId != null ? !stockId.equals(stock.stockId) : stock.stockId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return stockId != null ? stockId.hashCode() : 0;
    }
}
