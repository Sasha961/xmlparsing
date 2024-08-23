package com.myproject.xmlparsing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.internal.util.stereotypes.Lazy;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "price_file", catalog = "qwep_price_dev")
public class PriceFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    private String name;
    @OneToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "sender_id", referencedColumnName = "id")
    private UserEntity userEntity;
    @Lazy
    private byte[] file;

    public PriceFile(String name, UserEntity userEntity, byte[] file) {
        this.name = name;
        this.userEntity = userEntity;
        this.file = file;
    }
}
