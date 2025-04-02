package spring._3alemliveback.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity

public class Token {



        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String token;

        private boolean revoked;

        private boolean expired;

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public String getToken() {
                return token;
        }

        public void setToken(String token) {
                this.token = token;
        }

        public boolean isRevoked() {
                return revoked;
        }

        public void setRevoked(boolean revoked) {
                this.revoked = revoked;
        }

        public boolean isExpired() {
                return expired;
        }

        public void setExpired(boolean expired) {
                this.expired = expired;
        }

        public User getUser() {
                return user;
        }

        public void setUser(User user) {
                this.user = user;
        }

        @ManyToOne
        @JoinColumn(name = "user_id")
        private User user;
    }
