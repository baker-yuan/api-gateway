package storage

import (
	"context"
	"fmt"
	"time"

	"github.com/apisix/manager-api/internal/conf"
	"github.com/apisix/manager-api/internal/log"
	"github.com/apisix/manager-api/internal/utils"
	"github.com/apisix/manager-api/internal/utils/runtime"
	"go.etcd.io/etcd/client/pkg/v3/transport"
	clientv3 "go.etcd.io/etcd/client/v3"
)

const (
	// SkippedValueEtcdInitDir indicates the init_dir
	// etcd event will be skipped.
	SkippedValueEtcdInitDir = "init_dir"

	// SkippedValueEtcdEmptyObject indicates the data with an
	// empty JSON value {}, which may be set by APISIX,
	// should be also skipped.
	//
	// Important: at present, {} is considered as invalid,
	// but may be changed in the future.
	SkippedValueEtcdEmptyObject = "{}"
)

var (
	etcdClient *clientv3.Client // etcd客户端，InitETCDClient初始化赋值
)

type EtcdV3Storage struct {
	closing bool
	client  *clientv3.Client
}

func InitETCDClient(etcdConf *conf.Etcd) error {
	config := clientv3.Config{
		Endpoints:   etcdConf.Endpoints,
		DialTimeout: 5 * time.Second,
		Username:    etcdConf.Username,
		Password:    etcdConf.Password,
	}
	// mTLS
	if etcdConf.MTLS != nil && etcdConf.MTLS.CaFile != "" &&
		etcdConf.MTLS.CertFile != "" && etcdConf.MTLS.KeyFile != "" {
		tlsInfo := transport.TLSInfo{
			CertFile:      etcdConf.MTLS.CertFile,
			KeyFile:       etcdConf.MTLS.KeyFile,
			TrustedCAFile: etcdConf.MTLS.CaFile,
		}
		tlsConfig, err := tlsInfo.ClientConfig()
		if err != nil {
			return err
		}
		config.TLS = tlsConfig
	}

	cli, err := clientv3.New(config)
	if err != nil {
		log.Errorf("init etcd failed: %s", err)
		return fmt.Errorf("init etcd failed: %s", err)
	}

	etcdClient = cli
	utils.AppendToClosers(Close)
	return nil
}

func GenEtcdStorage() *EtcdV3Storage {
	return &EtcdV3Storage{
		client: etcdClient,
	}
}

func Close() error {
	if err := etcdClient.Close(); err != nil {
		log.Errorf("etcd client close failed: %s", err)
		return err
	}
	return nil
}

// Get 根据key查找
func (s *EtcdV3Storage) Get(ctx context.Context, key string) (string, error) {
	resp, err := s.client.Get(ctx, key)
	if err != nil {
		log.Errorf("etcd get failed: %s", err)
		return "", fmt.Errorf("etcd get failed: %s", err)
	}
	if resp.Count == 0 {
		log.Warnf("key: %s is not found", key)
		return "", fmt.Errorf("key: %s is not found", key)
	}
	return string(resp.Kvs[0].Value), nil
}

func (s *EtcdV3Storage) List(ctx context.Context, key string) ([]Keypair, error) {
	resp, err := s.client.Get(ctx, key, clientv3.WithPrefix())
	if err != nil {
		log.Errorf("etcd get failed: %s", err)
		return nil, fmt.Errorf("etcd get failed: %s", err)
	}
	var ret []Keypair
	for i := range resp.Kvs {
		key := string(resp.Kvs[i].Key)
		value := string(resp.Kvs[i].Value)

		// Skip the data if its value is init_dir or {}
		// during fetching-all phase.
		//
		// For more complex cases, an explicit function to determine if
		// skippable would be better.
		if value == SkippedValueEtcdInitDir || value == SkippedValueEtcdEmptyObject {
			continue
		}

		data := Keypair{
			Key:   key,
			Value: value,
		}
		ret = append(ret, data)
	}

	return ret, nil
}

func (s *EtcdV3Storage) Create(ctx context.Context, key, val string) error {
	_, err := s.client.Put(ctx, key, val)
	if err != nil {
		log.Errorf("etcd put failed: %s", err)
		return fmt.Errorf("etcd put failed: %s", err)
	}
	return nil
}

func (s *EtcdV3Storage) Update(ctx context.Context, key, val string) error {
	_, err := s.client.Put(ctx, key, val)
	if err != nil {
		log.Errorf("etcd put failed: %s", err)
		return fmt.Errorf("etcd put failed: %s", err)
	}
	return nil
}

func (s *EtcdV3Storage) BatchDelete(ctx context.Context, keys []string) error {
	for i := range keys {
		resp, err := s.client.Delete(ctx, keys[i])
		if err != nil {
			log.Errorf("delete etcd key[%s] failed: %s", keys[i], err)
			return fmt.Errorf("delete etcd key[%s] failed: %s", keys[i], err)
		}
		if resp.Deleted == 0 {
			log.Warnf("key: %s is not found", keys[i])
			return fmt.Errorf("key: %s is not found", keys[i])
		}
	}
	return nil
}

func (s *EtcdV3Storage) Watch(ctx context.Context, key string) <-chan WatchResponse {
	eventChan := s.client.Watch(ctx, key, clientv3.WithPrefix())
	ch := make(chan WatchResponse, 1)
	go func() {
		defer runtime.HandlePanic()
		for event := range eventChan {
			if event.Err() != nil {
				log.Errorf("etcd watch error: key: %s err: %v", key, event.Err())
				close(ch)
				return
			}

			// 构建通道传输的数据
			output := WatchResponse{
				Canceled: event.Canceled,
			}

			for i := range event.Events {
				key := string(event.Events[i].Kv.Key)
				value := string(event.Events[i].Kv.Value)

				// Skip the data if its value is init_dir or {}
				// during watching phase.
				//
				// For more complex cases, an explicit function to determine if
				// skippable would be better.
				if value == SkippedValueEtcdInitDir || value == SkippedValueEtcdEmptyObject {
					continue
				}

				e := Event{
					Keypair: Keypair{
						Key:   key,
						Value: value,
					},
				}
				switch event.Events[i].Type {
				case clientv3.EventTypePut:
					e.Type = EventTypePut
				case clientv3.EventTypeDelete:
					e.Type = EventTypeDelete
				}
				output.Events = append(output.Events, e)
			}
			if output.Canceled {
				log.Error("channel canceled")
				output.Error = fmt.Errorf("channel canceled")
			}
			ch <- output
		}

		close(ch)
	}()

	// 返回通道
	return ch
}

func (s *EtcdV3Storage) GetClient() *clientv3.Client {
	return s.client
}
