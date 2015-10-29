package test.hibernate;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class AnotherBean {
    @Id
    private Long id;

    private String value;

    // for hibernate
    AnotherBean() {
    }

    public AnotherBean(String value, Long id) {
        setValue(value);

        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
