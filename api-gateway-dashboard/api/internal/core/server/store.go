package server

import (
	"github.com/apisix/manager-api/internal/conf"
	"github.com/apisix/manager-api/internal/core/storage"
	"github.com/apisix/manager-api/internal/core/store"
	"github.com/apisix/manager-api/internal/log"
)

func (s *server) setupStore() error {
	if err := storage.InitETCDClient(conf.ETCDConfig); err != nil {
		log.Errorf("init etcd client fail: %v", err)
		return err
	}
	if err := store.InitStores(); err != nil {
		log.Errorf("init stores fail: %v", err)
		return err
	}
	return nil
}
